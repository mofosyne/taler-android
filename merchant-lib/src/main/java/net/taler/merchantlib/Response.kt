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

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.Serializable

class Response<out T> private constructor(
    private val value: Any?
) {
    companion object {
        suspend fun <T> response(request: suspend () -> T): Response<T> {
            return try {
                success(request())
            } catch (e: Throwable) {
                println(e)
                failure(e)
            }
        }

        fun <T> success(value: T): Response<T> =
            Response(value)

        fun <T> failure(e: Throwable): Response<T> =
            Response(Failure(e))
    }

    val isFailure: Boolean get() = value is Failure

    suspend fun handle(onFailure: ((String) -> Unit)? = null, onSuccess: ((T) -> Unit)? = null) {
        if (value is Failure) onFailure?.let { it(getFailureString(value)) }
        else onSuccess?.let {
            @Suppress("UNCHECKED_CAST")
            it(value as T)
        }
    }

    suspend fun handleSuspend(
        onFailure: ((String) -> Any)? = null,
        onSuccess: (suspend (T) -> Any)? = null
    ) {
        if (value is Failure) onFailure?.let { it(getFailureString(value)) }
        else onSuccess?.let {
            @Suppress("UNCHECKED_CAST")
            it(value as T)
        }
    }

    private suspend fun getFailureString(failure: Failure): String = when (failure.exception) {
        is ResponseException -> getExceptionString(failure.exception)
        else -> failure.exception.toString()
    }

    private suspend fun getExceptionString(e: ResponseException): String {
        val response = e.response
        return try {
            val error: Error = response.body()
            "Error ${error.code} (${response.status.value}): ${error.hint} ${error.detail}"
        } catch (ex: Exception) {
            "Status code: ${response.status.value}"
        }
    }

    private class Failure(val exception: Throwable)

    @Serializable
    private class Error(
        val code: Int?,
        val hint: String?,
        val detail: String? = null,
    )
}
