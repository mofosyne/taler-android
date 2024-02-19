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

package net.taler.cashier

import android.util.Log
import androidx.annotation.WorkerThread
import net.taler.cashier.config.Config
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONException
import org.json.JSONObject

object HttpHelper {

    private val TAG = HttpHelper::class.java.simpleName
    private const val MIME_TYPE_JSON = "application/json"

    @WorkerThread
    fun makeJsonGetRequest(url: String, config: Config): HttpJsonResult {
        val request = Request.Builder()
            .addHeader("Accept", MIME_TYPE_JSON)
            .url(url)
            .get()
            .build()
        val response = try {
            getHttpClient(config.username, config.password)
                .newCall(request)
                .execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving $url", e)
            return HttpJsonResult.Error(0)
        }
        return if (response.code == 204) {
            HttpJsonResult.Success(JSONObject())
        } else if (response.code in 200..299 && response.body != null) {
            val jsonObject = JSONObject(response.body!!.string())
            HttpJsonResult.Success(jsonObject)
        } else {
            Log.e(TAG, "Received status ${response.code} from $url expected 2xx")
            HttpJsonResult.Error(response.code, getErrorBody(response))
        }
    }

    private val MEDIA_TYPE_JSON = "$MIME_TYPE_JSON; charset=utf-8".toMediaTypeOrNull()

    @WorkerThread
    fun makeJsonPostRequest(url: String, body: JSONObject, config: Config): HttpJsonResult {
        val request = Request.Builder()
            .addHeader("Accept", MIME_TYPE_JSON)
            .url(url)
            .post(body.toString().toRequestBody(MEDIA_TYPE_JSON))
            .build()
        val response = try {
            getHttpClient(config.username, config.password)
                .newCall(request)
                .execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving $url", e)
            return HttpJsonResult.Error(0)
        }
        return if (response.code == 204) {
            HttpJsonResult.Success(JSONObject())
        } else if (response.code in 200..299 && response.body != null) {
            val jsonObject = JSONObject(response.body!!.string())
            HttpJsonResult.Success(jsonObject)
        } else {
            Log.e(TAG, "Received status ${response.code} from $url expected 2xx")
            HttpJsonResult.Error(response.code, getErrorBody(response))
        }
    }

    private fun getHttpClient(username: String, password: String) =
        OkHttpClient.Builder().authenticator(object : Authenticator {
            override fun authenticate(route: Route?, response: Response): Request? {
                val credential = Credentials.basic(username, password)
                if (credential == response.request.header("Authorization")) {
                    // If we already failed with these credentials, don't retry
                    return null
                }
                return response
                    .request
                    .newBuilder()
                    .header("Authorization", credential)
                    .build()
            }
        }).build()

    private fun getErrorBody(response: Response): String? {
        val body = response.body?.string() ?: return null
        Log.e(TAG, "Response body: $body")
        return try {
            val json = JSONObject(body)
            "${json.optString("ec")} ${json.optString("error")}"
        } catch (e: JSONException) {
            null
        }
    }

}

sealed class HttpJsonResult {
    class Error(val statusCode: Int, private val errorMsg: String? = null) : HttpJsonResult() {
        val msg: String
            get() = errorMsg?.let { "\n\n$statusCode $it" } ?: "$statusCode"
    }

    class Success(val json: JSONObject) : HttpJsonResult()
}
