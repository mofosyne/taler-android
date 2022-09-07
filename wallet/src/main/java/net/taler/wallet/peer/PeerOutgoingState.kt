/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.peer

import android.graphics.Bitmap
import kotlinx.serialization.Serializable
import net.taler.wallet.backend.TalerErrorInfo

sealed class PeerOutgoingState
object PeerOutgoingIntro : PeerOutgoingState()
object PeerOutgoingCreating : PeerOutgoingState()
data class PeerOutgoingResponse(
    val talerUri: String,
    val qrCode: Bitmap,
) : PeerOutgoingState()

data class PeerOutgoingError(
    val info: TalerErrorInfo,
) : PeerOutgoingState()

@Serializable
data class InitiatePeerPullPaymentResponse(
    /**
     * Taler URI for the other party to make the payment that was requested.
     */
    val talerUri: String,
)

@Serializable
data class InitiatePeerPushPaymentResponse(
    val exchangeBaseUrl: String,
    val talerUri: String,
)
