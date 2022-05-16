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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

public class AmountParserException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)
public class AmountOverflowException(msg: String? = null, cause: Throwable? = null) : Exception(msg, cause)

@Serializable(with = KotlinXAmountSerializer::class)
public data class Amount(
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
) : Comparable<Amount> {

    public companion object {

        private const val FRACTIONAL_BASE: Int = 100000000 // 1e8

        private val REGEX_CURRENCY = Regex("""^[-_*A-Za-z0-9]{1,12}$""")
        public val MAX_VALUE: Long = 2.0.pow(52).toLong()
        private const val MAX_FRACTION_LENGTH = 8
        public const val MAX_FRACTION: Int = 99_999_999

        public fun zero(currency: String): Amount {
            return Amount(checkCurrency(currency), 0, 0)
        }

        public fun fromJSONString(str: String): Amount {
            val split = str.split(":")
            if (split.size != 2) throw AmountParserException("Invalid Amount Format")
            return fromString(split[0], split[1])
        }

        public fun fromString(currency: String, str: String): Amount {
            // value
            val valueSplit = str.split(".")
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
            return Amount(checkCurrency(currency), value, fraction)
        }

        public fun min(currency: String): Amount = Amount(currency, 0, 1)
        public fun max(currency: String): Amount = Amount(currency, MAX_VALUE, MAX_FRACTION)


        internal fun checkCurrency(currency: String): String {
            if (!REGEX_CURRENCY.matches(currency))
                throw AmountParserException("Invalid currency: $currency")
            return currency
        }

        internal fun checkValue(value: Long?): Long {
            if (value == null || value > MAX_VALUE)
                throw AmountParserException("Value $value greater than $MAX_VALUE")
            return value
        }

        internal fun checkFraction(fraction: Int?): Int {
            if (fraction == null || fraction > MAX_FRACTION)
                throw AmountParserException("Fraction $fraction greater than $MAX_FRACTION")
            return fraction
        }

    }

    public val amountStr: String
        get() = if (fraction == 0) "$value" else {
            var f = fraction
            var fractionStr = ""
            while (f > 0) {
                fractionStr += f / (FRACTIONAL_BASE / 10)
                f = (f * 10) % FRACTIONAL_BASE
            }
            "$value.$fractionStr"
        }

    public operator fun plus(other: Amount): Amount {
        check(currency == other.currency) { "Can only subtract from same currency" }
        val resultValue = value + other.value + floor((fraction + other.fraction).toDouble() / FRACTIONAL_BASE).toLong()
        if (resultValue > MAX_VALUE)
            throw AmountOverflowException()
        val resultFraction = (fraction + other.fraction) % FRACTIONAL_BASE
        return Amount(currency, resultValue, resultFraction)
    }

    public operator fun times(factor: Int): Amount {
        // TODO consider replacing with a faster implementation
        if (factor == 0) return zero(currency)
        var result = this
        for (i in 1 until factor) result += this
        return result
    }

    public operator fun minus(other: Amount): Amount {
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

    public fun isZero(): Boolean {
        return value == 0L && fraction == 0
    }

    public fun toJSONString(): String {
        return "$currency:$amountStr"
    }

    override fun toString(): String {
        return "$amountStr $currency"
    }

    override fun compareTo(other: Amount): Int {
        check(currency == other.currency) { "Can only compare amounts with the same currency" }
        when {
            value == other.value -> {
                if (fraction < other.fraction) return -1
                if (fraction > other.fraction) return 1
                return 0
            }
            value < other.value -> return -1
            else -> return 1
        }
    }

}

@Suppress("EXPERIMENTAL_API_USAGE")
@Serializer(forClass = Amount::class)
internal object KotlinXAmountSerializer : KSerializer<Amount> {
    override fun serialize(encoder: Encoder, value: Amount) {
        encoder.encodeString(value.toJSONString())
    }

    override fun deserialize(decoder: Decoder): Amount {
        return Amount.fromJSONString(decoder.decodeString())
    }
}
