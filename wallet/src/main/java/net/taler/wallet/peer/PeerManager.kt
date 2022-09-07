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

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.taler.common.Amount
import net.taler.common.QrCodeManager
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeItem
import org.json.JSONObject

class PeerManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val _pullState = MutableStateFlow<PeerOutgoingState>(PeerOutgoingIntro)
    val pullState: StateFlow<PeerOutgoingState> = _pullState

    private val _pushState = MutableStateFlow<PeerOutgoingState>(PeerOutgoingIntro)
    val pushState: StateFlow<PeerOutgoingState> = _pushState

    private val _paymentState = MutableStateFlow<PeerIncomingState>(PeerIncomingChecking)
    val paymentState: StateFlow<PeerIncomingState> = _paymentState

    fun initiatePullPayment(amount: Amount, exchange: ExchangeItem) {
        _pullState.value = PeerOutgoingCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPullPayment", InitiatePeerPullPaymentResponse.serializer()) {
                put("exchangeBaseUrl", exchange.exchangeBaseUrl)
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", "test")
                })
            }.onSuccess {
                val qrCode = QrCodeManager.makeQrCode(it.talerUri)
                _pullState.value = PeerOutgoingResponse(it.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPullPayment error result $error")
                _pullState.value = PeerOutgoingError(error)
            }
        }
    }

    fun resetPullPayment() {
        _pullState.value = PeerOutgoingIntro
    }

    fun initiatePeerPushPayment(amount: Amount, summary: String) {
        _pushState.value = PeerOutgoingCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPushPayment", InitiatePeerPushPaymentResponse.serializer()) {
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", summary)
                })
            }.onSuccess { response ->
                val qrCode = QrCodeManager.makeQrCode(response.talerUri)
                _pushState.value = PeerOutgoingResponse(response.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPushPayment error result $error")
                _pushState.value = PeerOutgoingError(error)
            }
        }
    }

    fun resetPushPayment() {
        _pushState.value = PeerOutgoingIntro
    }

    fun checkPeerPullPayment(talerUri: String) {
        _paymentState.value = PeerIncomingChecking
        scope.launch(Dispatchers.IO) {
            api.request("checkPeerPullPayment", CheckPeerPullPaymentResponse.serializer()) {
                put("talerUri", talerUri)
            }.onSuccess { response ->
                _paymentState.value = PeerIncomingTerms(
                    amount = response.amount,
                    contractTerms = response.contractTerms,
                    id = response.peerPullPaymentIncomingId,
                )
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _paymentState.value = PeerIncomingError(error)
            }
        }
    }

    fun acceptPeerPullPayment(terms: PeerIncomingTerms) {
        _paymentState.value = PeerIncomingAccepting(terms)
        scope.launch(Dispatchers.IO) {
            api.request<Unit>("acceptPeerPullPayment") {
                put("peerPullPaymentIncomingId", terms.id)
            }.onSuccess {
                _paymentState.value = PeerIncomingAccepted
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _paymentState.value = PeerIncomingError(error)
            }
        }
    }

}
