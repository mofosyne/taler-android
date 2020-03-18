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

@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package net.taler.wallet

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import org.json.JSONObject
import kotlin.math.round

private const val FRACTIONAL_BASE = 1e8

@JsonDeserialize(using = AmountDeserializer::class)
data class Amount(val currency: String, val amount: String) {
    fun isZero(): Boolean {
        return amount.toDouble() == 0.0
    }

    companion object {
        fun fromJson(jsonAmount: JSONObject): Amount {
            val amountCurrency = jsonAmount.getString("currency")
            val amountValue = jsonAmount.getString("value")
            val amountFraction = jsonAmount.getString("fraction")
            val amountIntValue = Integer.parseInt(amountValue)
            val amountIntFraction = Integer.parseInt(amountFraction)
            return Amount(
                amountCurrency,
                (amountIntValue + amountIntFraction / FRACTIONAL_BASE).toString()
            )
        }

        fun fromString(strAmount: String): Amount {
            val components = strAmount.split(":")
            return Amount(components[0], components[1])
        }
    }

    override fun toString(): String {
        return String.format("%.2f $currency", amount.toDouble())
    }
}

class AmountDeserializer : StdDeserializer<Amount>(Amount::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Amount {
        val node = p.codec.readValue(p, String::class.java)
        return Amount.fromString(node)
    }
}

class ParsedAmount(
    /**
     * name of the currency using either a three-character ISO 4217 currency code,
     * or a regional currency identifier starting with a "*" followed by at most 10 characters.
     * ISO 4217 exponents in the name are not supported,
     * although the "fraction" is corresponds to an ISO 4217 exponent of 6.
     */
    val currency: String,

    /**
     * unsigned 32 bit value in the currency,
     * note that "1" here would correspond to 1 EUR or 1 USD, depending on currency, not 1 cent.
     */
    val value: UInt,

    /**
     * unsigned 32 bit fractional value to be added to value
     * representing an additional currency fraction,
     * in units of one millionth (1e-6) of the base currency value.
     * For example, a fraction of 500,000 would correspond to 50 cents.
     */
    val fraction: Double
) {
    companion object {
        fun parseAmount(str: String): ParsedAmount {
            val split = str.split(":")
            check(split.size == 2)
            val currency = split[0]
            val valueSplit = split[1].split(".")
            val value = valueSplit[0].toUInt()
            val fraction: Double = if (valueSplit.size > 1) {
                round("0.${valueSplit[1]}".toDouble() * FRACTIONAL_BASE)
            } else 0.0
            return ParsedAmount(currency, value, fraction)
        }
    }

    operator fun minus(other: ParsedAmount): ParsedAmount {
        check(currency == other.currency) { "Can only subtract from same currency" }
        var resultValue = value
        var resultFraction = fraction
        if (resultFraction < other.fraction) {
            if (resultValue < 1u) {
                return ParsedAmount(currency, 0u, 0.0)
            }
            resultValue--
            resultFraction += FRACTIONAL_BASE
        }
        check(resultFraction >= other.fraction)
        resultFraction -= other.fraction
        if (resultValue < other.value) {
            return ParsedAmount(currency, 0u, 0.0)
        }
        resultValue -= other.value
        return ParsedAmount(currency, resultValue, resultFraction)
    }

    fun isZero(): Boolean {
        return value == 0u && fraction == 0.0
    }

    @Suppress("unused")
    fun toJSONString(): String {
        return "$currency:${getValueString()}"
    }

    override fun toString(): String {
        return "${getValueString()} $currency"
    }

    private fun getValueString(): String {
        return "$value${(fraction / FRACTIONAL_BASE).toString().substring(1)}"
    }

}
