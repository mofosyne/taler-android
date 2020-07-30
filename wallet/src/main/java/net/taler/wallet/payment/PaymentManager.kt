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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.payment.PayStatus.AlreadyPaid
import net.taler.wallet.payment.PayStatus.InsufficientBalance
import net.taler.wallet.payment.PreparePayResponse.AlreadyConfirmedResponse
import net.taler.wallet.payment.PreparePayResponse.InsufficientBalanceResponse
import net.taler.wallet.payment.PreparePayResponse.PaymentPossibleResponse
import org.json.JSONObject
import java.net.MalformedURLException

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

    data class InsufficientBalance(val contractTerms: ContractTerms) : PayStatus()
    object AlreadyPaid : PayStatus()
    data class Error(val error: String) : PayStatus()
    data class Success(val currency: String) : PayStatus()
}

class PaymentManager(
    private val walletBackendApi: WalletBackendApi,
    private val mapper: ObjectMapper
) {

    private val mPayStatus = MutableLiveData<PayStatus>(PayStatus.None)
    internal val payStatus: LiveData<PayStatus> = mPayStatus

    private val mDetailsShown = MutableLiveData<Boolean>()
    internal val detailsShown: LiveData<Boolean> = mDetailsShown

    @UiThread
    fun preparePay(url: String) {
        mPayStatus.value = PayStatus.Loading
        mDetailsShown.value = false

        val args = JSONObject(mapOf("talerPayUri" to url))
        walletBackendApi.sendRequest("preparePay", args) { isError, result ->
            if (isError) {
                handleError("preparePay", result.toString(2))
                return@sendRequest
            }
            val response: PreparePayResponse = mapper.readValue(result.toString())
            Log.e(TAG, "PreparePayResponse $response")
            mPayStatus.value = when (response) {
                is PaymentPossibleResponse -> response.toPayStatusPrepared()
                is InsufficientBalanceResponse -> InsufficientBalance(response.contractTerms)
                is AlreadyConfirmedResponse -> AlreadyPaid
            }
        }
    }

    private fun getContractTerms(json: JSONObject): ContractTerms {
        val terms: ContractTerms = mapper.readValue(json.getString("contractTermsRaw"))
        // validate product images
        terms.products.forEach { product ->
            product.image?.let { image ->
                if (REGEX_PRODUCT_IMAGE.matchEntire(image) == null) {
                    throw MalformedURLException("Invalid image data URL for ${product.description}")
                }
            }
        }
        return terms
    }

    fun confirmPay(proposalId: String, currency: String) {
        val args = JSONObject(mapOf("proposalId" to proposalId))
        walletBackendApi.sendRequest("confirmPay", args) { isError, result ->
            if (isError) {
                handleError("preparePay", result.toString())
                return@sendRequest
            }
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

    internal fun abortProposal(proposalId: String) {
        val args = JSONObject(mapOf("proposalId" to proposalId))

        Log.i(TAG, "aborting proposal")

        walletBackendApi.sendRequest("abortProposal", args) { isError, result ->
            if (isError) {
                handleError("abortProposal", result.toString(2))
                Log.e(TAG, "received error response to abortProposal")
                return@sendRequest
            }
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

    private fun handleError(operation: String, msg: String) {
        Log.e(TAG, "got $operation error result $msg")
        mPayStatus.value = PayStatus.Error(msg)
    }

}
