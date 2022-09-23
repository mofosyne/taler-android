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

import java.util.Locale

public object TalerUri {

    private const val SCHEME = "taler://"
    private const val SCHEME_INSECURE = "taler+http://"
    private const val AUTHORITY_PAY = "pay"
    private const val AUTHORITY_WITHDRAW = "withdraw"
    private const val AUTHORITY_REFUND = "refund"
    private const val AUTHORITY_TIP = "tip"

    public data class WithdrawUriResult(
        val bankIntegrationApiBaseUrl: String,
        val withdrawalOperationId: String
    )

    /**
     * Parses a withdraw URI and returns a bank status URL or null if the URI was invalid.
     */
    public fun parseWithdrawUri(uri: String): WithdrawUriResult? {
        val (resultScheme, prefix) = when {
            uri.startsWith(SCHEME, ignoreCase = true) -> {
                Pair("https://", "${SCHEME}${AUTHORITY_WITHDRAW}/")
            }
            uri.startsWith(SCHEME_INSECURE, ignoreCase = true) -> {
                Pair("http://", "${SCHEME_INSECURE}${AUTHORITY_WITHDRAW}/")
            }
            else -> return null
        }
        if (!uri.startsWith(prefix, ignoreCase = true)) return null
        val parts = uri.let {
            (if (it.endsWith("/")) it.dropLast(1) else it).substring(prefix.length).split('/')
        }
        if (parts.size < 2) return null
        val host = parts[0].lowercase(Locale.ROOT)
        val pathSegments = parts.slice(1 until parts.size - 1).joinToString("/")
        val withdrawId = parts.last()
        if (withdrawId.isBlank()) return null
        val url = "${resultScheme}${host}/${pathSegments}"

        return WithdrawUriResult(url, withdrawId)
    }

}
