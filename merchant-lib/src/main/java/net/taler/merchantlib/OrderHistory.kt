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
import net.taler.common.Amount
import net.taler.common.Timestamp

@Serializable
data class OrderHistory(
    val orders: List<OrderHistoryEntry>
)

@Serializable
data class OrderHistoryEntry(
    // order ID of the transaction related to this entry.
    @SerialName("order_id")
    val orderId: String,

    // when the order was created
    val timestamp: Timestamp,

    // the amount of money the order is for
    val amount: Amount,

    // the summary of the order
    val summary: String,

    // if the order has been paid
    val paid: Boolean,

    // whether some part of the order is refundable
    val refundable: Boolean
)
