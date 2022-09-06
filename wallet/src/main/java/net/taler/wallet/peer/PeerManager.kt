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
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Amount
import net.taler.common.QrCodeManager
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeItem
import org.json.JSONObject

class PeerManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val _pullState = MutableStateFlow<PeerPaymentState>(PeerPaymentIntro)
    val pullState: StateFlow<PeerPaymentState> = _pullState

    private val _pushState = MutableStateFlow<PeerPaymentState>(PeerPaymentIntro)
    val pushState: StateFlow<PeerPaymentState> = _pushState

    fun initiatePullPayment(amount: Amount, exchange: ExchangeItem) {
        _pullState.value = PeerPaymentCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPullPayment", InitiatePeerPullPaymentResponse.serializer()) {
                put("exchangeBaseUrl", exchange.exchangeBaseUrl)
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", "test")
                })
            }.onSuccess {
                val qrCode = QrCodeManager.makeQrCode(it.talerUri)
                _pullState.value = PeerPaymentResponse(it.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPullPayment error result $error")
                _pullState.value = PeerPaymentError(error)
            }
        }
    }

    fun resetPullPayment() {
        _pullState.value = PeerPaymentIntro
    }

    fun initiatePeerPushPayment(amount: Amount, summary: String) {
        _pushState.value = PeerPaymentCreating
        scope.launch(Dispatchers.IO) {
            api.request("initiatePeerPushPayment", InitiatePeerPushPaymentResponse.serializer()) {
                put("amount", amount.toJSONString())
                put("partialContractTerms", JSONObject().apply {
                    put("summary", summary)
                })
            }.onSuccess { response ->
                val qrCode = QrCodeManager.makeQrCode(response.talerUri)
                _pushState.value = PeerPaymentResponse(response.talerUri, qrCode)
            }.onError { error ->
                Log.e(TAG, "got initiatePeerPushPayment error result $error")
                _pushState.value = PeerPaymentError(error)
            }
        }
    }

    fun resetPushPayment() {
        _pushState.value = PeerPaymentIntro
    }

}

sealed class PeerPaymentState
object PeerPaymentIntro : PeerPaymentState()
object PeerPaymentCreating : PeerPaymentState()
data class PeerPaymentResponse(
    val talerUri: String,
    val qrCode: Bitmap,
) : PeerPaymentState()

data class PeerPaymentError(
    val info: TalerErrorInfo,
) : PeerPaymentState()

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
