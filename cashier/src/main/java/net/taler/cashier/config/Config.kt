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

package net.taler.cashier.config

import kotlinx.serialization.Serializable
import okhttp3.Credentials

data class Config(
    val bankUrl: String,
    val username: String,
    val password: String
) {
    val basicAuth: String get() = Credentials.basic(username, password)
}

@Serializable
data class ConfigResponse(
    val version: String,
    val currency: String
)

sealed class ConfigResult {
    class Error(val authError: Boolean, val msg: String) : ConfigResult()
    object Offline : ConfigResult()
    object Success : ConfigResult()
}
