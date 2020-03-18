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
import net.taler.wallet.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject
import java.net.MalformedURLException

val REGEX_PRODUCT_IMAGE = Regex("^data:image/(jpeg|png);base64,([A-Za-z0-9+/=]+)$")

class PaymentManager(
    private val walletBackendApi: WalletBackendApi,
    private val mapper: ObjectMapper
) {

    private val mPayStatus = MutableLiveData<PayStatus>(PayStatus.None)
    internal val payStatus: LiveData<PayStatus> = mPayStatus

    private val mDetailsShown = MutableLiveData<Boolean>()
    internal val detailsShown: LiveData<Boolean> = mDetailsShown

    private var currentPayRequestId = 0

    @UiThread
    fun preparePay(url: String) {
        mPayStatus.value = PayStatus.Loading
        mDetailsShown.value = false

        val args = JSONObject(mapOf("url" to url))

        currentPayRequestId += 1
        val payRequestId = currentPayRequestId

        walletBackendApi.sendRequest("preparePay", args) { isError, result ->
            when {
                isError -> {
                    Log.v(TAG, "got preparePay error result")
                    mPayStatus.value = PayStatus.Error(result.toString())
                }
                payRequestId != this.currentPayRequestId -> {
                    Log.v(TAG, "preparePay result was for old request")
                }
                else -> {
                    val status = result.getString("status")
                    try {
                        mPayStatus.postValue(getPayStatusUpdate(status, result))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting PayStatusUpdate", e)
                        mPayStatus.postValue(PayStatus.Error(e.message ?: "unknown error"))
                    }
                }
            }
        }
    }

    private fun getPayStatusUpdate(status: String, json: JSONObject) = when (status) {
        "payment-possible" -> PayStatus.Prepared(
            contractTerms = getContractTerms(json),
            proposalId = json.getString("proposalId"),
            totalFees = Amount.fromJson(json.getJSONObject("totalFees"))
        )
        "paid" -> PayStatus.AlreadyPaid(getContractTerms(json))
        "insufficient-balance" -> PayStatus.InsufficientBalance(getContractTerms(json))
        "error" -> PayStatus.Error("got some error")
        else -> PayStatus.Error("unknown status")
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

    @UiThread
    fun toggleDetailsShown() {
        val oldValue = mDetailsShown.value ?: false
        mDetailsShown.value = !oldValue
    }

    fun confirmPay(proposalId: String) {
        val args = JSONObject(mapOf("proposalId" to proposalId))

        walletBackendApi.sendRequest("confirmPay", args) { _, _ ->
            mPayStatus.postValue(PayStatus.Success)
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

        walletBackendApi.sendRequest("abortProposal", args) { isError, _ ->
            if (isError) {
                Log.e(TAG, "received error response to abortProposal")
                return@sendRequest
            }
            mPayStatus.postValue(PayStatus.None)
        }
    }

    @UiThread
    fun resetPayStatus() {
        mPayStatus.value = PayStatus.None
    }

}

sealed class PayStatus {
    object None : PayStatus()
    object Loading : PayStatus()
    data class Prepared(
        val contractTerms: ContractTerms,
        val proposalId: String,
        val totalFees: Amount
    ) : PayStatus()

    data class InsufficientBalance(val contractTerms: ContractTerms) : PayStatus()
    data class AlreadyPaid(val contractTerms: ContractTerms) : PayStatus()
    data class Error(val error: String) : PayStatus()
    object Success : PayStatus()
}
