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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
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

    // Error details, type depends on talerErrorCode
    @Serializable(JSONObjectDeserializer::class)
    @JsonDeserialize(using = JsonObjectDeserializer::class)
    val details: JSONObject?
) {
    val userFacingMsg: String
        get() {
            return StringBuilder().apply {
                append(talerErrorCode)
                append(" ")
                append(message)
                details?.let { details ->
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

class JSONObjectDeserializer : KSerializer<JSONObject> {

    override val descriptor = PrimitiveSerialDescriptor("JSONObjectDeserializer", STRING)

    override fun deserialize(decoder: Decoder): JSONObject {
        val input = decoder as JsonDecoder
        val tree = input.decodeJsonElement() as JsonObject
        return JSONObject(tree.toString())
    }

    override fun serialize(encoder: Encoder, value: JSONObject) {
        error("not supported")
    }
}

class JsonObjectDeserializer : StdDeserializer<JSONObject>(JSONObject::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): JSONObject {
        val node: JsonNode = p.codec.readTree(p)
        return JSONObject(node.toString())
    }
}
