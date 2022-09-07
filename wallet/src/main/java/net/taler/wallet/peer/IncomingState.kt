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

import kotlinx.serialization.Serializable
import net.taler.common.Amount
import net.taler.wallet.backend.TalerErrorInfo

sealed class IncomingState
object IncomingChecking : IncomingState()
open class IncomingTerms(
    val amount: Amount,
    val contractTerms: PeerContractTerms,
    val id: String,
) : IncomingState()

class IncomingAccepting(s: IncomingTerms) :
    IncomingTerms(s.amount, s.contractTerms, s.id)

object IncomingAccepted : IncomingState()
data class IncomingError(
    val info: TalerErrorInfo,
) : IncomingState()

@Serializable
data class PeerContractTerms(
    val summary: String,
    val amount: Amount,
)

@Serializable
data class CheckPeerPullPaymentResponse(
    val amount: Amount,
    val contractTerms: PeerContractTerms,
    val peerPullPaymentIncomingId: String,
)

@Serializable
data class CheckPeerPushPaymentResponse(
    val amount: Amount,
    val contractTerms: PeerContractTerms,
    val peerPushPaymentIncomingId: String,
)
