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

package net.taler.merchantlib

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MerchantConfig(
    @SerialName("base_url")
    val baseUrl: String,
    // TODO remove instance when it is part of baseURL
    val instance: String? = null,
    @SerialName("api_key")
    val apiKey: String
) {
    fun urlFor(endpoint: String): String {
        val sb = StringBuilder(baseUrl)
        if (sb.last() != '/') sb.append('/')
        sb.append("instances/$instance/")
        sb.append(endpoint)
        return sb.toString()
    }
}
