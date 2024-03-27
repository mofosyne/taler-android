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

package net.taler.wallet.exchanges

import kotlinx.serialization.Serializable
import net.taler.wallet.balances.ScopeInfo
import net.taler.wallet.cleanExchange

@Serializable
data class BuiltinExchange(
    val exchangeBaseUrl: String,
    val currencyHint: String? = null,
)

@Serializable
data class ExchangeItem(
    val exchangeBaseUrl: String,
    // can be null before exchange info in wallet-core was fully loaded
    val currency: String? = null,
    val paytoUris: List<String>,
    val scopeInfo: ScopeInfo? = null,
) {
    val name: String get() = cleanExchange(exchangeBaseUrl)
}