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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

@Serializable
sealed class WalletResponse<T> {
    @Serializable
    @SerialName("response")
    data class Success<T>(
        val result: T,
    ) : WalletResponse<T>()

    @Serializable
    @SerialName("error")
    data class Error<T>(
        val error: TalerErrorInfo,
    ) : WalletResponse<T>()

    fun onSuccess(block: (result: T) -> Unit): WalletResponse<T> {
        if (this is Success) block(this.result)
        return this
    }

    fun onError(block: (result: TalerErrorInfo) -> Unit): WalletResponse<T> {
        if (this is Error) block(this.error)
        return this
    }
}

@Serializable(with = TalerErrorInfoDeserializer::class)
data class TalerErrorInfo(
    // Numeric error code defined defined in the
    // GANA gnu-taler-error-codes registry.
    val code: TalerErrorCode,

    // English description of the error code.
    val hint: String? = null,

    // English diagnostic message that can give details
    // for the instance of the error.
    val message: String? = null,

    // Error extra details
    val extra: Map<String, JsonElement> = mapOf(),
) {
    val userFacingMsg: String
        get() {
            return StringBuilder().apply {
                hint?.let { append(it) }
                message?.let { append(" ").append(it) }
            }.toString()
        }

    fun getStringExtra(key: String): String? =
        extra[key]?.jsonPrimitive?.content
}

class TalerErrorInfoDeserializer : KSerializer<TalerErrorInfo> {
    private val stringToJsonElementSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

    override val descriptor: SerialDescriptor
        get() = stringToJsonElementSerializer.descriptor

    override fun deserialize(decoder: Decoder): TalerErrorInfo {
        // Decoder -> JsonInput
        require(decoder is JsonDecoder)
        val json = decoder.json
        val filtersMap = decoder.decodeSerializableValue(stringToJsonElementSerializer)

        val code = filtersMap["code"]?.let {
            json.decodeFromJsonElement(TalerErrorCode.serializer(), it)
        } ?: TalerErrorCode.UNKNOWN
        val hint = filtersMap["hint"]?.let {
            json.decodeFromJsonElement(String.serializer(), it)
        }
        val message = filtersMap["message"]?.let {
            json.decodeFromJsonElement(String.serializer(), it)
        }

        val knownKeys = setOf("code", "hint", "message")
        val unknownFilters = filtersMap.filter { (key, _) -> !knownKeys.contains(key) }

        return TalerErrorInfo(code, hint, message, unknownFilters)
    }

    override fun serialize(encoder: Encoder, value: TalerErrorInfo) {
        error("not supported")
    }
}
