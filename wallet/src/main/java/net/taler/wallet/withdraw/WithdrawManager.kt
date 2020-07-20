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
import net.taler.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails
import org.json.JSONObject

sealed class WithdrawStatus {
    data class Loading(val talerWithdrawUri: String) : WithdrawStatus()
    data class TermsOfServiceReviewRequired(
        val talerWithdrawUri: String,
        val exchange: String,
        val tosText: String,
        val tosEtag: String,
        val amount: Amount,
        val fee: Amount
    ) : WithdrawStatus()

    data class ReceivedDetails(
        val talerWithdrawUri: String,
        val exchange: String,
        val amount: Amount,
        val fee: Amount
    ) : WithdrawStatus()

    data class Withdrawing(val talerWithdrawUri: String) : WithdrawStatus()
    data class Success(val currency: String) : WithdrawStatus()
    data class Error(val message: String?) : WithdrawStatus()
}

class WithdrawManager(private val walletBackendApi: WalletBackendApi) {

    val withdrawStatus = MutableLiveData<WithdrawStatus>()
    val testWithdrawalInProgress = MutableLiveData(false)

    var exchangeFees: ExchangeFees? = null
        private set

    fun withdrawTestkudos() {
        testWithdrawalInProgress.value = true

        walletBackendApi.sendRequest("withdrawTestkudos", null) { _, _ ->
            testWithdrawalInProgress.postValue(false)
        }
    }

    fun getWithdrawalDetails(exchangeItem: ExchangeItem, amount: Amount) {
        val args = JSONObject().apply {
            put("exchangeBaseUrl", exchangeItem.exchangeBaseUrl)
            put("amount", amount.toJSONString())
        }
        walletBackendApi.sendRequest("getWithdrawalDetailsForAmount", args) { isError, result ->
            // {"rawAmount":"TESTKUDOS:5","effectiveAmount":"TESTKUDOS:4.8","paytoUris":["payto:\/\/x-taler-bank\/bank.test.taler.net\/Exchange"],"tosAccepted":false}
            if (isError) {
                Log.e(TAG, "$result")
            } else {
                Log.e(TAG, "$result")
            }
        }
    }

    fun getWithdrawalInfo(talerWithdrawUri: String) {
        val args = JSONObject().apply {
            put("talerWithdrawUri", talerWithdrawUri)
        }
        withdrawStatus.value = WithdrawStatus.Loading(talerWithdrawUri)

        walletBackendApi.sendRequest("getWithdrawDetailsForUri", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error getWithdrawDetailsForUri ${result.toString(4)}")
                val message = if (result.has("message")) result.getString("message") else null
                withdrawStatus.postValue(WithdrawStatus.Error(message))
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
        val args = JSONObject().apply {
            put("talerWithdrawUri", talerWithdrawUri)
            put("selectedExchange", selectedExchange)
        }

        walletBackendApi.sendRequest("getWithdrawDetailsForUri", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error getWithdrawDetailsForUri ${result.toString(4)}")
                val message = if (result.has("message")) result.getString("message") else null
                withdrawStatus.postValue(WithdrawStatus.Error(message))
                return@sendRequest
            }
            Log.v(TAG, "got getWithdrawDetailsForUri result (with exchange details)")
            val status = withdrawStatus.value
            if (status !is WithdrawStatus.Loading) {
                Log.w(TAG, "ignoring withdrawal info result, not loading.")
                return@sendRequest
            }
            val wi = result.getJSONObject("bankWithdrawDetails")
            val amount = Amount.fromJsonObject(wi.getJSONObject("amount"))

            val ei = result.getJSONObject("exchangeWithdrawDetails")
            val termsOfServiceAccepted = ei.getBoolean("termsOfServiceAccepted")

            exchangeFees = ExchangeFees.fromExchangeWithdrawDetailsJson(ei)

            val withdrawFee = Amount.fromJsonObject(ei.getJSONObject("withdrawFee"))
            val overhead = Amount.fromJsonObject(ei.getJSONObject("overhead"))
            val fee = withdrawFee + overhead

            if (!termsOfServiceAccepted) {
                val exchange = ei.getJSONObject("exchangeInfo")
                val tosText = exchange.getString("termsOfServiceText")
                val tosEtag = exchange.optString("termsOfServiceLastEtag", "undefined")
                withdrawStatus.postValue(
                    WithdrawStatus.TermsOfServiceReviewRequired(
                        status.talerWithdrawUri,
                        selectedExchange, tosText, tosEtag,
                        amount, fee
                    )
                )
            } else {
                withdrawStatus.postValue(
                    ReceivedDetails(
                        status.talerWithdrawUri,
                        selectedExchange, amount,
                        fee
                    )
                )
            }
        }
    }

    fun acceptWithdrawal(talerWithdrawUri: String, selectedExchange: String, currency: String) {
        val args = JSONObject()
        args.put("talerWithdrawUri", talerWithdrawUri)
        args.put("selectedExchange", selectedExchange)

        withdrawStatus.value = WithdrawStatus.Withdrawing(talerWithdrawUri)

        walletBackendApi.sendRequest("acceptWithdrawal", args) { isError, result ->
            if (isError) {
                Log.v(TAG, "got acceptWithdrawal error result: ${result.toString(2)}")
                return@sendRequest
            }
            Log.v(TAG, "got acceptWithdrawal result")
            val status = withdrawStatus.value
            if (status !is WithdrawStatus.Withdrawing) {
                Log.w(TAG, "ignoring acceptWithdrawal result, invalid state: $status")
                return@sendRequest
            }
            withdrawStatus.postValue(WithdrawStatus.Success(currency))
        }
    }

    /**
     * Accept the currently displayed terms of service.
     */
    fun acceptCurrentTermsOfService() {
        val s = withdrawStatus.value
        check(s is WithdrawStatus.TermsOfServiceReviewRequired)

        val args = JSONObject().apply {
            put("exchangeBaseUrl", s.exchange)
            put("etag", s.tosEtag)
        }
        walletBackendApi.sendRequest("acceptExchangeTermsOfService", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error acceptExchangeTermsOfService ${result.toString(4)}")
                return@sendRequest
            }
            val status = ReceivedDetails(s.talerWithdrawUri, s.exchange, s.amount, s.fee)
            withdrawStatus.postValue(status)
        }
    }

}
