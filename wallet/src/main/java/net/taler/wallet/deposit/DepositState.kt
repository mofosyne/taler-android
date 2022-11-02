/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.deposit

import net.taler.common.Amount

sealed class DepositState {

    open val showFees: Boolean = false
    open val effectiveDepositAmount: Amount? = null

    object Start : DepositState()
    object CheckingFees : DepositState()
    class FeesChecked(
        override val effectiveDepositAmount: Amount,
    ) : DepositState() {
        override val showFees = true
    }

    class MakingDeposit(
        override val effectiveDepositAmount: Amount,
    ) : DepositState() {
        override val showFees = true
    }

    object Success : DepositState()

    class Error(val msg: String) : DepositState()

}
