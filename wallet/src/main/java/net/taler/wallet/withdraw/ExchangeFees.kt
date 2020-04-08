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

package net.taler.wallet.withdraw

import net.taler.common.Amount
import net.taler.common.Timestamp
import org.json.JSONObject

data class CoinFee(
    val coin: Amount,
    val feeDeposit: Amount,
    val feeRefresh: Amount,
    val feeRefund: Amount,
    val feeWithdraw: Amount
)

data class CoinFees(
    val quantity: Int,
    val coinFee: CoinFee
)

data class WireFee(
    val start: Timestamp,
    val end: Timestamp,
    val wireFee: Amount,
    val closingFee: Amount
)

data class ExchangeFees(
    val withdrawFee: Amount,
    val overhead: Amount,
    val earliestDepositExpiration: Timestamp,
    val coinFees: List<CoinFees>,
    val wireFees: List<WireFee>
) {
    companion object {
        fun fromExchangeWithdrawDetailsJson(json: JSONObject): ExchangeFees {
            val earliestDepositExpiration =
                json.getJSONObject("earliestDepositExpiration").getLong("t_ms")

            val selectedDenoms = json.getJSONArray("selectedDenoms")
            val coinFees = HashMap<CoinFee, Int>(selectedDenoms.length())
            for (i in 0 until selectedDenoms.length()) {
                val denom = selectedDenoms.getJSONObject(i)
                val coinFee = CoinFee(
                    coin = Amount.fromJsonObject(denom.getJSONObject("value")),
                    feeDeposit = Amount.fromJsonObject(denom.getJSONObject("feeDeposit")),
                    feeRefresh = Amount.fromJsonObject(denom.getJSONObject("feeRefresh")),
                    feeRefund = Amount.fromJsonObject(denom.getJSONObject("feeRefund")),
                    feeWithdraw = Amount.fromJsonObject(denom.getJSONObject("feeWithdraw"))
                )
                coinFees[coinFee] = (coinFees[coinFee] ?: 0) + 1
            }

            val wireFeesJson = json.getJSONObject("wireFees")
            val feesForType = wireFeesJson.getJSONObject("feesForType")
            val bankFees = feesForType.getJSONArray("x-taler-bank")
            val wireFees = ArrayList<WireFee>(bankFees.length())
            for (i in 0 until bankFees.length()) {
                val fee = bankFees.getJSONObject(i)
                val startStamp =
                    fee.getJSONObject("startStamp").getLong("t_ms")
                val endStamp =
                    fee.getJSONObject("endStamp").getLong("t_ms")
                val wireFee = WireFee(
                    start = Timestamp(startStamp),
                    end = Timestamp(endStamp),
                    wireFee = Amount.fromJsonObject(fee.getJSONObject("wireFee")),
                    closingFee = Amount.fromJsonObject(fee.getJSONObject("closingFee"))
                )
                wireFees.add(wireFee)
            }

            return ExchangeFees(
                withdrawFee = Amount.fromJsonObject(json.getJSONObject("withdrawFee")),
                overhead = Amount.fromJsonObject(json.getJSONObject("overhead")),
                earliestDepositExpiration = Timestamp(earliestDepositExpiration),
                coinFees = coinFees.map { (coinFee, quantity) ->
                    CoinFees(quantity, coinFee)
                },
                wireFees = wireFees
            )
        }
    }
}
