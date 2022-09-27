/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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
import kotlinx.serialization.json.JsonObject

@Serializable
sealed class ApiMessage {

    @Serializable
    @SerialName("notification")
    data class Notification(
        val payload: NotificationPayload,
    ) : ApiMessage()

}

@Serializable
data class NotificationPayload(
    val type: String,
    val id: String? = null,
)

@Serializable
sealed class ApiResponse : ApiMessage() {

    abstract val id: Int
    abstract val operation: String

    @Serializable
    @SerialName("response")
    data class Response(
        override val id: Int,
        override val operation: String,
        val result: JsonObject,
    ) : ApiResponse()

    @Serializable
    @SerialName("error")
    data class Error(
        override val id: Int,
        override val operation: String,
        val error: JsonObject,
    ) : ApiResponse()
}
