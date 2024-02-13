/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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

package net.taler.wallet.balances

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.Amount

@Serializable
data class BalanceItem(
    val scopeInfo: ScopeInfo,
    val available: Amount,
    val pendingIncoming: Amount,
    val pendingOutgoing: Amount,
) {
    val currency: String get() = available.currency
    val hasPending: Boolean get() = !pendingIncoming.isZero() || !pendingOutgoing.isZero()
}

@Serializable
sealed class ScopeInfo {
    abstract val currency: String

    @Serializable
    @SerialName("global")
    data class Global(
        override val currency: String
    ): ScopeInfo()

    @Serializable
    @SerialName("exchange")
    data class Exchange(
        override val currency: String,
        val url: String,
    ): ScopeInfo()

    @Serializable
    @SerialName("auditor")
    data class Auditor(
        override val currency: String,
        val url: String,
    ): ScopeInfo()
}