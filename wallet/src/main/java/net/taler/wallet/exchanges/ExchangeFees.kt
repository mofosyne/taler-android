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

package net.taler.wallet.exchanges

import net.taler.common.Amount
import net.taler.common.Timestamp


data class CoinFee(
    val coin: Amount,
    val quantity: Int,
    val feeDeposit: Amount,
    val feeRefresh: Amount,
    val feeRefund: Amount,
    val feeWithdraw: Amount
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
    val coinFees: List<CoinFee>,
    val wireFees: List<WireFee>
)
