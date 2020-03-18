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

package net.taler.wallet.withdraw

import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.taler.wallet.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

sealed class WithdrawStatus {
    data class Loading(val talerWithdrawUri: String) : WithdrawStatus()
    data class TermsOfServiceReviewRequired(
        val talerWithdrawUri: String,
        val exchangeBaseUrl: String,
        val tosText: String,
        val tosEtag: String,
        val amount: Amount,
        val suggestedExchange: String
    ) : WithdrawStatus()

    data class ReceivedDetails(
        val talerWithdrawUri: String,
        val amount: Amount,
        val suggestedExchange: String
    ) : WithdrawStatus()

    data class Withdrawing(val talerWithdrawUri: String) : WithdrawStatus()

    object Success : WithdrawStatus()
    data class Error(val message: String?) : WithdrawStatus()
}

class WithdrawManager(private val walletBackendApi: WalletBackendApi) {

    val withdrawStatus = MutableLiveData<WithdrawStatus>()
    val testWithdrawalInProgress = MutableLiveData(false)

    private var currentWithdrawRequestId = 0

    fun withdrawTestkudos() {
        testWithdrawalInProgress.value = true

        walletBackendApi.sendRequest("withdrawTestkudos", null) { _, _ ->
            testWithdrawalInProgress.postValue(false)
        }
    }

    fun getWithdrawalInfo(talerWithdrawUri: String) {
        val args = JSONObject()
        args.put("talerWithdrawUri", talerWithdrawUri)

        withdrawStatus.value = WithdrawStatus.Loading(talerWithdrawUri)

        this.currentWithdrawRequestId++
        val myWithdrawRequestId = this.currentWithdrawRequestId

        walletBackendApi.sendRequest("getWithdrawDetailsForUri", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error getWithdrawDetailsForUri ${result.toString(4)}")
                val message = if (result.has("message")) result.getString("message") else null
                withdrawStatus.postValue(WithdrawStatus.Error(message))
                return@sendRequest
            }
            if (myWithdrawRequestId != this.currentWithdrawRequestId) {
                val mismatch = "$myWithdrawRequestId != ${this.currentWithdrawRequestId}"
                Log.w(TAG, "Got withdraw result for different request id $mismatch")
                return@sendRequest
            }
            Log.v(TAG, "got getWithdrawDetailsForUri result")
            val status = withdrawStatus.value
            if (status !is WithdrawStatus.Loading) {
                Log.v(TAG, "ignoring withdrawal info result, not loading.")
                return@sendRequest
            }
            val wi = result.getJSONObject("bankWithdrawDetails")
            val suggestedExchange = wi.getString("suggestedExchange")
            // We just use the suggested exchange, in the future there will be
            // a selection dialog.
            getWithdrawalInfoWithExchange(talerWithdrawUri, suggestedExchange)
        }
    }

    private fun getWithdrawalInfoWithExchange(talerWithdrawUri: String, selectedExchange: String) {
        val args = JSONObject()
        args.put("talerWithdrawUri", talerWithdrawUri)
        args.put("selectedExchange", selectedExchange)

        this.currentWithdrawRequestId++
        val myWithdrawRequestId = this.currentWithdrawRequestId

        walletBackendApi.sendRequest("getWithdrawDetailsForUri", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error getWithdrawDetailsForUri ${result.toString(4)}")
                val message = if (result.has("message")) result.getString("message") else null
                withdrawStatus.postValue(WithdrawStatus.Error(message))
                return@sendRequest
            }
            if (myWithdrawRequestId != this.currentWithdrawRequestId) {
                val mismatch = "$myWithdrawRequestId != ${this.currentWithdrawRequestId}"
                Log.w(TAG, "Got withdraw result for different request id $mismatch")
                return@sendRequest
            }
            Log.v(TAG, "got getWithdrawDetailsForUri result (with exchange details)")
            val status = withdrawStatus.value
            if (status !is WithdrawStatus.Loading) {
                Log.v(TAG, "ignoring withdrawal info result, not loading.")
                return@sendRequest
            }
            val wi = result.getJSONObject("bankWithdrawDetails")
            val suggestedExchange = wi.getString("suggestedExchange")
            val amount = Amount.fromJson(wi.getJSONObject("amount"))

            val ei = result.getJSONObject("exchangeWithdrawDetails")
            val termsOfServiceAccepted = ei.getBoolean("termsOfServiceAccepted")

            if (!termsOfServiceAccepted) {
                val exchange = ei.getJSONObject("exchangeInfo")
                val tosText = exchange.getString("termsOfServiceText")
                val tosEtag = exchange.optString("termsOfServiceLastEtag", "undefined")
                withdrawStatus.postValue(
                    WithdrawStatus.TermsOfServiceReviewRequired(
                        status.talerWithdrawUri,
                        selectedExchange,
                        tosText,
                        tosEtag,
                        amount,
                        suggestedExchange
                    )
                )
            } else {
                withdrawStatus.postValue(
                    WithdrawStatus.ReceivedDetails(
                        status.talerWithdrawUri,
                        amount,
                        suggestedExchange
                    )
                )
            }
        }
    }

    fun acceptWithdrawal(talerWithdrawUri: String, selectedExchange: String) {
        val args = JSONObject()
        args.put("talerWithdrawUri", talerWithdrawUri)
        args.put("selectedExchange", selectedExchange)

        withdrawStatus.value = WithdrawStatus.Withdrawing(talerWithdrawUri)

        walletBackendApi.sendRequest("acceptWithdrawal", args) { isError, _ ->
            if (isError) {
                Log.v(TAG, "got acceptWithdrawal error result")
                return@sendRequest
            }
            Log.v(TAG, "got acceptWithdrawal result")
            val status = withdrawStatus.value
            if (status !is WithdrawStatus.Withdrawing) {
                Log.v(TAG, "ignoring acceptWithdrawal result, invalid state")
            }
            withdrawStatus.postValue(WithdrawStatus.Success)
        }
    }

    /**
     * Accept the currently displayed terms of service.
     */
    fun acceptCurrentTermsOfService() {
        when (val s = withdrawStatus.value) {
            is WithdrawStatus.TermsOfServiceReviewRequired -> {
                val args = JSONObject()
                args.put("exchangeBaseUrl", s.exchangeBaseUrl)
                args.put("etag", s.tosEtag)
                walletBackendApi.sendRequest("acceptExchangeTermsOfService", args) { isError, _ ->
                    if (isError) {
                        return@sendRequest
                    }
                    withdrawStatus.postValue(
                        WithdrawStatus.ReceivedDetails(
                            s.talerWithdrawUri,
                            s.amount,
                            s.suggestedExchange
                        )
                    )
                }
            }
        }
    }

    fun cancelCurrentWithdraw() {
        currentWithdrawRequestId++
        withdrawStatus.value = null
    }

}
