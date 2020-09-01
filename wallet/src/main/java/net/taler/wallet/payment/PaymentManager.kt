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

package net.taler.wallet.payment

import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.common.ContractTerms
import net.taler.lib.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.payment.PayStatus.AlreadyPaid
import net.taler.wallet.payment.PayStatus.InsufficientBalance
import net.taler.wallet.payment.PreparePayResponse.AlreadyConfirmedResponse
import net.taler.wallet.payment.PreparePayResponse.InsufficientBalanceResponse
import net.taler.wallet.payment.PreparePayResponse.PaymentPossibleResponse

val REGEX_PRODUCT_IMAGE = Regex("^data:image/(jpeg|png);base64,([A-Za-z0-9+/=]+)$")

sealed class PayStatus {
    object None : PayStatus()
    object Loading : PayStatus()
    data class Prepared(
        val contractTerms: ContractTerms,
        val proposalId: String,
        val amountRaw: Amount,
        val amountEffective: Amount
    ) : PayStatus()

    data class InsufficientBalance(
        val contractTerms: ContractTerms,
        val amountRaw: Amount
    ) : PayStatus()

    // TODO bring user to fulfilment URI
    object AlreadyPaid : PayStatus()
    data class Error(val error: String) : PayStatus()
    data class Success(val currency: String) : PayStatus()
}

class PaymentManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mPayStatus = MutableLiveData<PayStatus>(PayStatus.None)
    internal val payStatus: LiveData<PayStatus> = mPayStatus

    private val mDetailsShown = MutableLiveData<Boolean>()
    internal val detailsShown: LiveData<Boolean> = mDetailsShown

    @UiThread
    fun preparePay(url: String) = scope.launch {
        mPayStatus.value = PayStatus.Loading
        mDetailsShown.value = false
        api.request("preparePay", PreparePayResponse.serializer()) {
            put("talerPayUri", url)
        }.onError {
            handleError("preparePay", it)
        }.onSuccess { response ->
            mPayStatus.value = when (response) {
                is PaymentPossibleResponse -> response.toPayStatusPrepared()
                is InsufficientBalanceResponse -> InsufficientBalance(
                    response.contractTerms,
                    response.amountRaw
                )
                is AlreadyConfirmedResponse -> AlreadyPaid
            }
        }
    }

    fun confirmPay(proposalId: String, currency: String) = scope.launch {
        api.request("confirmPay", ConfirmPayResult.serializer()) {
            put("proposalId", proposalId)
        }.onError {
            handleError("confirmPay", it)
        }.onSuccess {
            mPayStatus.postValue(PayStatus.Success(currency))
        }
    }

    @UiThread
    fun abortPay() {
        val ps = payStatus.value
        if (ps is PayStatus.Prepared) {
            abortProposal(ps.proposalId)
        }
        resetPayStatus()
    }

    internal fun abortProposal(proposalId: String) = scope.launch {
        Log.i(TAG, "aborting proposal")
        api.request<Unit>("abortProposal") {
            put("proposalId", proposalId)
        }.onError {
            Log.e(TAG, "received error response to abortProposal")
            handleError("abortProposal", it)
        }.onSuccess {
            mPayStatus.postValue(PayStatus.None)
        }
    }

    @UiThread
    fun toggleDetailsShown() {
        val oldValue = mDetailsShown.value ?: false
        mDetailsShown.value = !oldValue
    }

    @UiThread
    fun resetPayStatus() {
        mPayStatus.value = PayStatus.None
    }

    private fun handleError(operation: String, error: TalerErrorInfo) {
        Log.e(TAG, "got $operation error result $error")
        mPayStatus.value = PayStatus.Error(error.userFacingMsg)
    }

}
