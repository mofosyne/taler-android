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
import java.time.LocalDateTime


@Serializable(with = ObservabilityEventSerializer::class)
class ObservabilityEvent(
    val body: JsonObject,
    val timestamp: LocalDateTime,
    val type: String,
)

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
            timestamp = LocalDateTime.now(),
            type = type,
        )
    }

    override fun serialize(encoder: Encoder, value: ObservabilityEvent) {
        encoder.encodeSerializableValue(JsonObject.serializer(), value.body)
    }
}