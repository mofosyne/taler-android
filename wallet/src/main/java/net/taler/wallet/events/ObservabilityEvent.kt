/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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

package net.taler.wallet.events

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.taler.wallet.R


@Serializable(with = ObservabilityEventSerializer::class)
class ObservabilityEvent(
    val body: JsonObject,
    val type: String,
) {

    fun getTitle(c: Context) = when (type) {
        "http-fetch-start" -> c.getString(R.string.event_http_fetch_start)
        "http-fetch-finish-error" -> c.getString(R.string.event_http_fetch_finish_error)
        "http-fetch-finish-success" -> c.getString(R.string.event_http_fetch_finish_success)
        "db-query-start" -> c.getString(R.string.event_db_query_start)
        "db-query-finish-success" -> c.getString(R.string.event_db_query_finish_success)
        "db-query-finish-error" -> c.getString(R.string.event_db_query_finish_error)
        "request-start" -> c.getString(R.string.event_request_start)
        "request-finish-success" -> c.getString(R.string.event_request_finish_success)
        "request-finish-error" -> c.getString(R.string.event_request_finish_error)
        "task-start" -> c.getString(R.string.event_task_start)
        "task-stop" -> c.getString(R.string.event_task_stop)
        "task-reset" -> c.getString(R.string.event_task_reset)
        "sheperd-task-result" -> c.getString(R.string.event_shepherd_task_result)
        "declare-task-dependency" -> c.getString(R.string.event_declare_task_dependency)
        "crypto-start" -> c.getString(R.string.event_crypto_start)
        "crypto-finish-success" -> c.getString(R.string.event_crypto_finish_success)
        "crypto-finish-error" -> c.getString(R.string.event_crypto_finish_error)
        "unknown" -> c.getString(R.string.event_unknown)
        else -> type
    }
}

class ObservabilityEventSerializer: KSerializer<ObservabilityEvent> {
    private val jsonElementSerializer = JsonElement.serializer()

    override val descriptor: SerialDescriptor
        get() = jsonElementSerializer.descriptor

    override fun deserialize(decoder: Decoder): ObservabilityEvent {
        require(decoder is JsonDecoder)
        val jsonObject = decoder
            .decodeJsonElement()
            .jsonObject

        val type = jsonObject["type"]
            ?.jsonPrimitive
            ?.content
            ?: "unknown"

        return ObservabilityEvent(
            body = jsonObject,
            type = type,
        )
    }

    override fun serialize(encoder: Encoder, value: ObservabilityEvent) {
        encoder.encodeSerializableValue(JsonObject.serializer(), value.body)
    }
}