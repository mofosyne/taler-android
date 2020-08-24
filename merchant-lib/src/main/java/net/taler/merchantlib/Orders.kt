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

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import net.taler.common.ContractTerms
import net.taler.lib.common.Duration

@Serializable
data class PostOrderRequest(
    @SerialName("order")
    val contractTerms: ContractTerms,
    @SerialName("refund_delay")
    val refundDelay: Duration? = null
)

@Serializable
data class PostOrderResponse(
    @SerialName("order_id")
    val orderId: String
)

@Serializable
sealed class CheckPaymentResponse {
    abstract val paid: Boolean

    @Suppress("EXPERIMENTAL_API_USAGE")
    @Serializer(forClass = CheckPaymentResponse::class)
    companion object : KSerializer<CheckPaymentResponse> {
        override fun deserialize(decoder: Decoder): CheckPaymentResponse {
            val input = decoder as JsonDecoder
            val tree = input.decodeJsonElement() as JsonObject
            val orderStatus = tree.getValue("order_status").jsonPrimitive.content
//            return if (orderStatus == "paid") decoder.json.decodeFromJsonElement(Paid.serializer(), tree)
//            else decoder.json.decodeFromJsonElement(Unpaid.serializer(), tree)
            // manual parsing due to https://github.com/Kotlin/kotlinx.serialization/issues/576
            return if (orderStatus == "paid") Paid(
                refunded = tree.getValue("refunded").jsonPrimitive.boolean
            ) else Unpaid(
                talerPayUri = tree.getValue("taler_pay_uri").jsonPrimitive.content
            )
        }

        override fun serialize(encoder: Encoder, value: CheckPaymentResponse) = when (value) {
            is Unpaid -> Unpaid.serializer().serialize(encoder, value)
            is Paid -> Paid.serializer().serialize(encoder, value)
        }
    }

    @Serializable
    data class Unpaid(
        override val paid: Boolean = false,
        @SerialName("taler_pay_uri")
        val talerPayUri: String,
        @SerialName("already_paid_order_id")
        val alreadyPaidOrderId: String? = null
    ) : CheckPaymentResponse()

    @Serializable
    data class Paid(
        override val paid: Boolean = true,
        val refunded: Boolean
    ) : CheckPaymentResponse()

}
