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
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.payment.PayStatus.AlreadyPaid
import net.taler.wallet.payment.PayStatus.InsufficientBalance
import net.taler.wallet.payment.PreparePayResponse.AlreadyConfirmedResponse
import net.taler.wallet.payment.PreparePayResponse.InsufficientBalanceResponse
import net.taler.wallet.payment.PreparePayResponse.PaymentPossibleResponse
import org.json.JSONObject

val REGEX_PRODUCT_IMAGE = Regex("^data:image/(jpeg|png);base64,([A-Za-z0-9+/=]+)$")

sealed class PayStatus {
    object None : PayStatus()
    object Loading : PayStatus()
    data class Prepared(
        val contractTerms: ContractTerms,
        val transactionId: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
    ) : PayStatus()

    data class InsufficientBalance(
        val contractTerms: ContractTerms,
        val amountRaw: Amount,
    ) : PayStatus()

    data class AlreadyPaid(
        val transactionId: String,
    ) : PayStatus()

    data class Pending(
        val transactionId: String? = null,
        val error: TalerErrorInfo? = null,
    ) : PayStatus()
    data class Success(
        val transactionId: String,
        val currency: String,
    ) : PayStatus()
}

class PaymentManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mPayStatus = MutableLiveData<PayStatus>(PayStatus.None)
    internal val payStatus: LiveData<PayStatus> = mPayStatus

    @UiThread
    fun preparePay(url: String) = scope.launch {
        mPayStatus.value = PayStatus.Loading
        api.request("preparePayForUri", PreparePayResponse.serializer()) {
            put("talerPayUri", url)
        }.onError {
            handleError("preparePayForUri", it)
        }.onSuccess { response ->
            mPayStatus.value = when (response) {
                is PaymentPossibleResponse -> response.toPayStatusPrepared()
                is InsufficientBalanceResponse -> InsufficientBalance(
                    contractTerms = response.contractTerms,
                    amountRaw = response.amountRaw
                )
                is AlreadyConfirmedResponse -> AlreadyPaid(
                    transactionId = response.transactionId,
                )
            }
        }
    }

    fun confirmPay(transactionId: String, currency: String) = scope.launch {
        api.request("confirmPay", ConfirmPayResult.serializer()) {
            put("transactionId", transactionId)
        }.onError {
            handleError("confirmPay", it)
        }.onSuccess { response ->
            mPayStatus.postValue(when (response) {
                is ConfirmPayResult.Done -> PayStatus.Success(
                    transactionId = response.transactionId,
                    currency = currency,
                )
                is ConfirmPayResult.Pending -> PayStatus.Pending(
                    transactionId = response.transactionId,
                    error = response.lastError,
                )
            })
        }
    }

    fun preparePayForTemplate(url: String, summary: String?, amount: Amount?) = scope.launch {
        mPayStatus.value = PayStatus.Loading
        api.request("preparePayForTemplate", PreparePayResponse.serializer()) {
            put("talerPayTemplateUri", url)
            put("templateParams", JSONObject().apply {
                summary?.let { put("summary", it) }
                amount?.let { put("amount", it.toJSONString()) }
            })
        }.onError {
            handleError("preparePayForTemplate", it)
        }.onSuccess { response ->
            mPayStatus.value = when (response) {
                is PaymentPossibleResponse -> response.toPayStatusPrepared()
                is InsufficientBalanceResponse -> InsufficientBalance(
                    contractTerms = response.contractTerms,
                    amountRaw = response.amountRaw,
                )

                is AlreadyConfirmedResponse -> AlreadyPaid(
                    transactionId = response.transactionId,
                )
            }
        }
    }

    @UiThread
    fun resetPayStatus() {
        mPayStatus.value = PayStatus.None
    }

    private fun handleError(operation: String, error: TalerErrorInfo) {
        Log.e(TAG, "got $operation error result $error")
        mPayStatus.value = PayStatus.Pending(error = error)
    }

}
