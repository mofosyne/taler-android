/*
 * This file is part of GNU Taler
 * (C) 2020 Taler Systems S.A.
 *
 * GNU Taler is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * GNU Taler is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GNU Taler; see the file COPYING.  If not, see <http://www.gnu.org/licenses/>
 */

package net.taler.common

import android.annotation.SuppressLint
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.json.JSONObject
import java.lang.Math.floorDiv
import kotlin.math.pow
import kotlin.math.roundToInt

class AmountDeserializer : StdDeserializer<Amount>(Amount::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Amount {
        val node = p.codec.readValue(p, String::class.java)
        try {
            return Amount.fromJSONString(node)
        } catch (e: AmountParserException) {
            throw JsonMappingException(p, "Error parsing Amount", e)
        }
    }
}

class AmountParserException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
class AmountOverflowException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)

@JsonDeserialize(using = AmountDeserializer::class)
data class Amount(
    /**
     * name of the currency using either a three-character ISO 4217 currency code,
     * or a regional currency identifier starting with a "*" followed by at most 10 characters.
     * ISO 4217 exponents in the name are not supported,
     * although the "fraction" is corresponds to an ISO 4217 exponent of 6.
     */
    val currency: String,

    /**
     * The integer part may be at most 2^52.
     * Note that "1" here would correspond to 1 EUR or 1 USD, depending on currency, not 1 cent.
     */
    val value: Long,

    /**
     * Unsigned 32 bit fractional value to be added to value representing
     * an additional currency fraction, in units of one hundred millionth (1e-8)
     * of the base currency value.  For example, a fraction
     * of 50_000_000 would correspond to 50 cents.
     */
    val fraction: Int
) {

    companion object {

        private const val FRACTIONAL_BASE: Int = 100000000 // 1e8

        @Suppress("unused")
        private val REGEX = Regex("""^[-_*A-Za-z0-9]{1,12}:([0-9]+)\.?([0-9]+)?$""")
        private val REGEX_CURRENCY = Regex("""^[-_*A-Za-z0-9]{1,12}$""")
        private val MAX_VALUE = 2.0.pow(52)
        private const val MAX_FRACTION_LENGTH = 8
        private const val MAX_FRACTION = 99_999_999

        @Throws(AmountParserException::class)
        @SuppressLint("CheckedExceptions")
        fun zero(currency: String): Amount {
            return Amount(checkCurrency(currency), 0, 0)
        }

        @Throws(AmountParserException::class)
        @SuppressLint("CheckedExceptions")
        fun fromJSONString(str: String): Amount {
            val split = str.split(":")
            if (split.size != 2) throw AmountParserException("Invalid Amount Format")
            // currency
            val currency = checkCurrency(split[0])
            // value
            val valueSplit = split[1].split(".")
            val value = checkValue(valueSplit[0].toLongOrNull())
            // fraction
            val fraction: Int = if (valueSplit.size > 1) {
                val fractionStr = valueSplit[1]
                if (fractionStr.length > MAX_FRACTION_LENGTH)
                    throw AmountParserException("Fraction $fractionStr too long")
                val fraction = "0.$fractionStr".toDoubleOrNull()
                    ?.times(FRACTIONAL_BASE)
                    ?.roundToInt()
                checkFraction(fraction)
            } else 0
            return Amount(currency, value, fraction)
        }

        @Throws(AmountParserException::class)
        @SuppressLint("CheckedExceptions")
        fun fromJsonObject(json: JSONObject): Amount {
            val currency = checkCurrency(json.optString("currency"))
            val value = checkValue(json.optString("value").toLongOrNull())
            val fraction = checkFraction(json.optString("fraction").toIntOrNull())
            return Amount(currency, value, fraction)
        }

        @Throws(AmountParserException::class)
        private fun checkCurrency(currency: String): String {
            if (!REGEX_CURRENCY.matches(currency))
                throw AmountParserException("Invalid currency: $currency")
            return currency
        }

        @Throws(AmountParserException::class)
        private fun checkValue(value: Long?): Long {
            if (value == null || value > MAX_VALUE)
                throw AmountParserException("Value $value greater than $MAX_VALUE")
            return value
        }

        @Throws(AmountParserException::class)
        private fun checkFraction(fraction: Int?): Int {
            if (fraction == null || fraction > MAX_FRACTION)
                throw AmountParserException("Fraction $fraction greater than $MAX_FRACTION")
            return fraction
        }

    }

    val amountStr: String
        get() = if (fraction == 0) "$value" else {
            var f = fraction
            var fractionStr = ""
            while (f > 0) {
                fractionStr += f / (FRACTIONAL_BASE / 10)
                f = (f * 10) % FRACTIONAL_BASE
            }
            "$value.$fractionStr"
        }

    @Throws(AmountOverflowException::class)
    operator fun plus(other: Amount): Amount {
        check(currency == other.currency) { "Can only subtract from same currency" }
        val resultValue = value + other.value + floorDiv(fraction + other.fraction, FRACTIONAL_BASE)
        if (resultValue > MAX_VALUE)
            throw AmountOverflowException()
        val resultFraction = (fraction + other.fraction) % FRACTIONAL_BASE
        return Amount(currency, resultValue, resultFraction)
    }

    @Throws(AmountOverflowException::class)
    operator fun times(factor: Int): Amount {
        var result = this
        for (i in 1 until factor) result += this
        return result
    }

    @Throws(AmountOverflowException::class)
    operator fun minus(other: Amount): Amount {
        check(currency == other.currency) { "Can only subtract from same currency" }
        var resultValue = value
        var resultFraction = fraction
        if (resultFraction < other.fraction) {
            if (resultValue < 1L)
                throw AmountOverflowException()
            resultValue--
            resultFraction += FRACTIONAL_BASE
        }
        check(resultFraction >= other.fraction)
        resultFraction -= other.fraction
        if (resultValue < other.value)
            throw AmountOverflowException()
        resultValue -= other.value
        return Amount(currency, resultValue, resultFraction)
    }

    fun isZero(): Boolean {
        return value == 0L && fraction == 0
    }

    fun toJSONString(): String {
        return "$currency:$amountStr"
    }

    override fun toString(): String {
        return "$amountStr $currency"
    }

}
