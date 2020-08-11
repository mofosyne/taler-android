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

package net.taler.wallet.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONObject

@Serializable
sealed class WalletResponse<T> {
    @Serializable
    @SerialName("response")
    data class Success<T>(
        val result: T
    ) : WalletResponse<T>()

    @Serializable
    @SerialName("error")
    data class Error<T>(
        val error: WalletErrorInfo
    ) : WalletResponse<T>()

    fun onSuccess(block: (result: T) -> Unit): WalletResponse<T> {
        if (this is Success) block(this.result)
        return this
    }

    fun onError(block: (result: WalletErrorInfo) -> Unit): WalletResponse<T> {
        if (this is Error) block(this.error)
        return this
    }
}

@Serializable
data class WalletErrorInfo(
    // Numeric error code defined defined in the
    // GANA gnu-taler-error-codes registry.
    val talerErrorCode: Int,

    // English description of the error code.
    val talerErrorHint: String,

    // English diagnostic message that can give details
    // for the instance of the error.
    val message: String,

    // Error details, type depends
    // on talerErrorCode
    val details: String?
) {
    val userFacingMsg: String
        get() {
            return StringBuilder().apply {
                append(talerErrorCode)
                append(" ")
                append(message)
                details?.let { it ->
                    val details = JSONObject(it)
                    details.optJSONObject("errorResponse")?.let { errorResponse ->
                        append("\n\n")
                        append(errorResponse.optString("code"))
                        append(" ")
                        append(errorResponse.optString("hint"))
                    }
                }
            }.toString()
        }
}
