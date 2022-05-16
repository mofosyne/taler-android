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
@Serializable
data class RefundRequest(
    /**
     * Amount to be refunded
     */
    val refund: Amount,

    /**
     * Human-readable refund justification
     */
    val reason: String
)

@Serializable
data class RefundResponse(
    /**
     * URL (handled by the backend) that the wallet should access to trigger refund processing.
     */
    @SerialName("taler_refund_uri")
    val talerRefundUri: String
)
