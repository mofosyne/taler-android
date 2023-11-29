/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.wallet.currency

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyInfo(
    val trustedAuditors: List<TrustedAuditor>,
    val trustedExchanges: List<TrustedExchange>,
)

@Serializable
data class TrustedAuditor(
    val currency: String,
    val auditorPub: String,
    val auditorBaseUrl: String,
)

@Serializable
data class TrustedExchange(
    val currency: String,
    val exchangeMasterPub: String,
    val exchangeBaseUrl: String,
)