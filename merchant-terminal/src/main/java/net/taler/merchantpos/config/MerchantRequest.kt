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


import android.util.ArrayMap
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class MerchantRequest(
    method: Int,
    private val merchantConfig: MerchantConfig,
    endpoint: String,
    params: Map<String, String>?,
    jsonRequest: JSONObject?,
    listener: Response.Listener<JSONObject>,
    errorListener: Response.ErrorListener
) :
    JsonObjectRequest(method, merchantConfig.urlFor(endpoint, params), jsonRequest, listener, errorListener) {

    override fun getHeaders(): MutableMap<String, String> {
        val headerMap = ArrayMap<String, String>()
        headerMap["Authorization"] = "ApiKey " + merchantConfig.apiKey
        return headerMap
    }
}
