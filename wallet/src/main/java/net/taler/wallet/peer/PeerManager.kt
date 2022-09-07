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

    private val _outgoingPullState = MutableStateFlow<OutgoingState>(OutgoingIntro)
    val pullState: StateFlow<OutgoingState> = _outgoingPullState

    private val _outgoingPushState = MutableStateFlow<OutgoingState>(OutgoingIntro)
    val pushState: StateFlow<OutgoingState> = _outgoingPushState

    private val _incomingPullState = MutableStateFlow<IncomingState>(IncomingChecking)
    val incomingPullState: StateFlow<IncomingState> = _incomingPullState

    private val _incomingPushState = MutableStateFlow<IncomingState>(IncomingChecking)
    val incomingPushState: StateFlow<IncomingState> = _incomingPushState

    fun initiatePullPayment(amount: Amount, exchange: ExchangeItem) {
        _outgoingPullState.value = OutgoingCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPullPayment", InitiatePeerPullPaymentResponse.serializer()) {
                put("exchangeBaseUrl", exchange.exchangeBaseUrl)
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", "test")
                })
            }.onSuccess {
                val qrCode = QrCodeManager.makeQrCode(it.talerUri)
                _outgoingPullState.value = OutgoingResponse(it.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPullPayment error result $error")
                _outgoingPullState.value = OutgoingError(error)
            }
        }
    }

    fun resetPullPayment() {
        _outgoingPullState.value = OutgoingIntro
    }

    fun initiatePeerPushPayment(amount: Amount, summary: String) {
        _outgoingPushState.value = OutgoingCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPushPayment", InitiatePeerPushPaymentResponse.serializer()) {
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", summary)
                })
            }.onSuccess { response ->
                val qrCode = QrCodeManager.makeQrCode(response.talerUri)
                _outgoingPushState.value = OutgoingResponse(response.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPushPayment error result $error")
                _outgoingPushState.value = OutgoingError(error)
            }
        }
    }

    fun resetPushPayment() {
        _outgoingPushState.value = OutgoingIntro
    }

    fun checkPeerPullPayment(talerUri: String) {
        _incomingPullState.value = IncomingChecking
        scope.launch(Dispatchers.IO) {
            api.request("checkPeerPullPayment", CheckPeerPullPaymentResponse.serializer()) {
                put("talerUri", talerUri)
            }.onSuccess { response ->
                _incomingPullState.value = IncomingTerms(
                    amount = response.amount,
                    contractTerms = response.contractTerms,
                    id = response.peerPullPaymentIncomingId,
                )
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _incomingPullState.value = IncomingError(error)
            }
        }
    }

    fun acceptPeerPullPayment(terms: IncomingTerms) {
        _incomingPullState.value = IncomingAccepting(terms)
        scope.launch(Dispatchers.IO) {
            api.request<Unit>("acceptPeerPullPayment") {
                put("peerPullPaymentIncomingId", terms.id)
            }.onSuccess {
                _incomingPullState.value = IncomingAccepted
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _incomingPullState.value = IncomingError(error)
            }
        }
    }

    fun checkPeerPushPayment(talerUri: String) {
        _incomingPushState.value = IncomingChecking
        scope.launch(Dispatchers.IO) {
            api.request("checkPeerPushPayment", CheckPeerPushPaymentResponse.serializer()) {
                put("talerUri", talerUri)
            }.onSuccess { response ->
                _incomingPushState.value = IncomingTerms(
                    amount = response.amount,
                    contractTerms = response.contractTerms,
                    id = response.peerPushPaymentIncomingId,
                )
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _incomingPushState.value = IncomingError(error)
            }
        }
    }

    fun acceptPeerPushPayment(terms: IncomingTerms) {
        _incomingPushState.value = IncomingAccepting(terms)
        scope.launch(Dispatchers.IO) {
            api.request<Unit>("acceptPeerPushPayment") {
                put("peerPushPaymentIncomingId", terms.id)
            }.onSuccess {
                _incomingPushState.value = IncomingAccepted
            }.onError { error ->
                Log.e(TAG, "got checkPeerPushPayment error result $error")
                _incomingPushState.value = IncomingError(error)
            }
        }
    }

}
