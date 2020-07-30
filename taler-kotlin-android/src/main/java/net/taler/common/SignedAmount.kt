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

data class SignedAmount(
    val positive: Boolean,
    val amount: Amount
) {

    companion object {
        @Throws(AmountParserException::class)
        @SuppressLint("CheckedExceptions")
        fun fromJSONString(str: String): SignedAmount = when (str.substring(0, 1)) {
            "-" -> SignedAmount(false, Amount.fromJSONString(str.substring(1)))
            "+" -> SignedAmount(true, Amount.fromJSONString(str.substring(1)))
            else -> SignedAmount(true, Amount.fromJSONString(str))
        }
    }

    override fun toString(): String {
        return if (positive) "$amount" else "-$amount"
    }

}