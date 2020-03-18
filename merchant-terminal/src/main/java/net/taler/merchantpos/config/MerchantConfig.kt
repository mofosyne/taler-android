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

package net.taler.merchantpos.config

import android.net.Uri
import com.fasterxml.jackson.annotation.JsonProperty

data class Config(
    val configUrl: String,
    val username: String,
    val password: String
) {
    fun isValid() = !configUrl.isBlank()
    fun hasPassword() = !password.isBlank()
}

data class MerchantConfig(
    @JsonProperty("base_url")
    val baseUrl: String,
    val instance: String,
    @JsonProperty("api_key")
    val apiKey: String,
    val currency: String?
) {
    fun urlFor(endpoint: String, params: Map<String, String>?): String {
        val uriBuilder = Uri.parse(baseUrl).buildUpon()
        uriBuilder.appendPath(endpoint)
        params?.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        return uriBuilder.toString()
    }
}
