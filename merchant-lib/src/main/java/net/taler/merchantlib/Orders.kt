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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.ContractTerms
import net.taler.common.Duration
import net.taler.lib.android.CustomClassDiscriminator

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
sealed class CheckPaymentResponse: CustomClassDiscriminator {
    override val discriminator: String = "order_status"
    abstract val paid: Boolean

    @Serializable
    @SerialName("unpaid")
    data class Unpaid(
        override val paid: Boolean = false,
        @SerialName("taler_pay_uri")
        val talerPayUri: String,
        @SerialName("already_paid_order_id")
        val alreadyPaidOrderId: String? = null
    ) : CheckPaymentResponse()

    @Serializable
    @SerialName("claimed")
    data class Claimed(
        override val paid: Boolean = false,
    ) : CheckPaymentResponse()

    @Serializable
    @SerialName("paid")
    data class Paid(
        override val paid: Boolean = true,
        val refunded: Boolean
    ) : CheckPaymentResponse()

}
