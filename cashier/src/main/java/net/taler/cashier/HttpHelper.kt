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
import okhttp3.Credentials
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
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
            return HttpJsonResult.Error(500)
        }
        return if (response.code() == 200 && response.body() != null) {
            val jsonObject = JSONObject(response.body()!!.string())
            HttpJsonResult.Success(jsonObject)
        } else {
            Log.e(TAG, "Received status ${response.code()} from $url expected 200")
            HttpJsonResult.Error(response.code())
        }
    }

    private val MEDIA_TYPE_JSON = MediaType.parse("$MIME_TYPE_JSON; charset=utf-8")

    @WorkerThread
    fun makeJsonPostRequest(url: String, body: JSONObject, config: Config): HttpJsonResult {
        val request = Request.Builder()
            .addHeader("Accept", MIME_TYPE_JSON)
            .url(url)
            .post(RequestBody.create(MEDIA_TYPE_JSON, body.toString()))
            .build()
        val response = try {
            getHttpClient(config.username, config.password)
                .newCall(request)
                .execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving $url", e)
            return HttpJsonResult.Error(500)
        }
        return if (response.code() == 200 && response.body() != null) {
            val jsonObject = JSONObject(response.body()!!.string())
            HttpJsonResult.Success(jsonObject)
        } else {
            Log.e(TAG, "Received status ${response.code()} from $url expected 200")
            HttpJsonResult.Error(response.code())
        }
    }

    private fun getHttpClient(username: String, password: String) =
        OkHttpClient.Builder().authenticator { _, response ->
            val credential = Credentials.basic(username, password)
            if (credential == response.request().header("Authorization")) {
                // If we already failed with these credentials, don't retry
                return@authenticator null
            }
            response
                .request()
                .newBuilder()
                .header("Authorization", credential)
                .build()
        }.build()

}

sealed class HttpJsonResult {
    class Error(val statusCode: Int) : HttpJsonResult()
    class Success(val json: JSONObject) : HttpJsonResult()
}
