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

import org.json.JSONObject

data class Amount(val currency: String, val amount: String) {

    companion object {
        private const val FRACTIONAL_BASE = 1e8
        private val SIGNED_REGEX = Regex("""([+\-])(\w+):([0-9.]+)""")

        fun fromString(strAmount: String): Amount {
            val components = strAmount.split(":")
            return Amount(components[0], components[1])
        }

        fun fromStringSigned(strAmount: String): Amount? {
            val groups = SIGNED_REGEX.matchEntire(strAmount)?.groupValues ?: emptyList()
            if (groups.size < 4) return null
            var amount = groups[3].toDoubleOrNull() ?: return null
            if (groups[1] == "-") amount *= -1
            val currency = groups[2]
            val amountStr = amount.toString()
            // only display as many digits as required to precisely render the balance
            return Amount(currency, amountStr.removeSuffix(".0"))
        }

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
    }

    fun isZero(): Boolean {
        return amount.toDouble() == 0.0
    }

    override fun toString() = "$amount $currency"

}
