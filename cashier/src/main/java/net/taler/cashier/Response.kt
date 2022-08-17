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

import android.content.Context
import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import net.taler.common.isOnline
import java.net.UnknownHostException

class Response<out T> private constructor(
    private val value: Any?
) {
    companion object {
        suspend fun <T> response(request: suspend () -> T): Response<T> {
            return try {
                Response(request())
            } catch (e: Throwable) {
                Log.e("HttpClient", "Error getting request", e)
                Response(getFailure(e))
            }
        }

        private suspend fun getFailure(e: Throwable): Failure = when (e) {
            is ResponseException -> Failure(e, getExceptionString(e), e.response.status)
            else -> Failure(e, e.toString())
        }

        private suspend fun getExceptionString(e: ResponseException): String {
            val response = e.response
            return try {
                val error: Error = response.body()
                "Error ${error.code}: ${error.hint}"
            } catch (ex: Exception) {
                "Status code: ${response.status.value}"
            }
        }
    }

    val isFailure: Boolean get() = value is Failure

    suspend fun onSuccess(block: suspend (result: T) -> Unit): Response<T> {
        @Suppress("UNCHECKED_CAST")
        if (!isFailure) block(value as T)
        return this
    }

    suspend fun onError(block: suspend (failure: Failure) -> Unit): Response<T> {
        if (value is Failure) block(value)
        return this
    }

    data class Failure(
        val exception: Throwable,
        val msg: String,
        val statusCode: HttpStatusCode? = null
    ) {
        fun isOffline(context: Context): Boolean {
            return exception is UnknownHostException && !context.isOnline()
        }
    }

    @Serializable
    private class Error(
        val code: Int?,
        val hint: String?
    )
}
