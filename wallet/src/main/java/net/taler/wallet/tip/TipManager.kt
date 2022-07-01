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

package net.taler.wallet.tip

import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.tip.PrepareTipResponse.TipPossibleResponse
import net.taler.wallet.tip.PrepareTipResponse.AlreadyAcceptedResponse

sealed class TipStatus {
    object None : TipStatus()
    object Loading : TipStatus()
    data class Prepared(
        val walletTipId: String,
        val merchantBaseUrl: String,
        val exchangeBaseUrl: String,
        val expirationTimestamp: Timestamp,
        val tipAmountRaw: Amount,
        val tipAmountEffective: Amount,
    ) : TipStatus()

    data class AlreadyAccepted(
        val walletTipId: String,
    ) : TipStatus()

    // TODO bring user to fulfilment URI
    data class Error(val error: String) : TipStatus()
    data class Success(val currency: String) : TipStatus()
}

class TipManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mTipStatus = MutableLiveData<TipStatus>(TipStatus.None)
    internal val tipStatus: LiveData<TipStatus> = mTipStatus

    @UiThread
    fun prepareTip(url: String) = scope.launch {
        mTipStatus.value = TipStatus.Loading
        api.request("prepareTip", PrepareTipResponse.serializer()) {
            put("talerTipUri", url)
        }.onError {
            handleError("prepareTip", it)
        }.onSuccess { response ->
            mTipStatus.value = when (response) {
                is TipPossibleResponse -> response.toTipStatusPrepared()
                is AlreadyAcceptedResponse -> TipStatus.AlreadyAccepted(
                    response.walletTipId
                )
            }
        }
    }

    fun confirmTip(tipId: String, currency: String) = scope.launch {
        api.request("acceptTip", ConfirmTipResult.serializer()) {
            put("walletTipId", tipId)
        }.onError {
            handleError("acceptTip", it)
        }.onSuccess {
            mTipStatus.postValue(TipStatus.Success(currency))
        }
    }

/*
    @UiThread
    fun abortTip() {
        val ps = tipStatus.value
        if (ps is TipStatus.Prepared) {
            abortProposal(ps.walletTipId)
        }
        resetTipStatus()
    }
*/

/*
    internal fun abortProposal(proposalId: String) = scope.launch {
        Log.i(TAG, "aborting proposal")
        api.request<Unit>("abortProposal") {
            put("proposalId", proposalId)
        }.onError {
            Log.e(TAG, "received error response to abortProposal")
            handleError("abortProposal", it)
        }.onSuccess {
            mTipStatus.postValue(TipStatus.None)
        }
    }
*/

    @UiThread
    fun resetTipStatus() {
        mTipStatus.value = TipStatus.None
    }

    private fun handleError(operation: String, error: TalerErrorInfo) {
        Log.e(TAG, "got $operation error result $error")
        mTipStatus.value = TipStatus.Error(error.userFacingMsg)
    }

}
