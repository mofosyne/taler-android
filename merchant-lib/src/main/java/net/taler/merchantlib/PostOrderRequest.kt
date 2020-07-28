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

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject
import net.taler.common.ContractTerms

@Serializable
data class PostOrderRequest(
    @SerialName("order")
    val contractTerms: ContractTerms

)

@Serializable
data class PostOrderResponse(
    @SerialName("order_id")
    val orderId: String
)

@Serializable
sealed class CheckPaymentResponse {
    abstract val paid: Boolean

    @Serializer(forClass = CheckPaymentResponse::class)
    companion object : KSerializer<CheckPaymentResponse> {
        override fun deserialize(decoder: Decoder): CheckPaymentResponse {
            val input = decoder as JsonInput
            val tree = input.decodeJson() as JsonObject
            val orderStatus = tree.getPrimitive("order_status").content
//            return if (orderStatus == "paid") decoder.json.fromJson(Paid.serializer(), tree)
//            else decoder.json.fromJson(Unpaid.serializer(), tree)
            // manual parsing due to https://github.com/Kotlin/kotlinx.serialization/issues/576
            return if (orderStatus == "paid") Paid(
                refunded = tree.getPrimitive("refunded").boolean
            ) else Unpaid(
                talerPayUri = tree.getPrimitive("taler_pay_uri").content
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
