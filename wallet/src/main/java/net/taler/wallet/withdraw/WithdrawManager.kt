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
import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeFees
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.getErrorString
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails
import org.json.JSONObject

sealed class WithdrawStatus {
    data class Loading(val talerWithdrawUri: String? = null) : WithdrawStatus()
    data class TosReviewRequired(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val tosText: String,
        val tosEtag: String
    ) : WithdrawStatus()

    data class ReceivedDetails(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount
    ) : WithdrawStatus()

    object Withdrawing : WithdrawStatus()
    data class Success(val currency: String) : WithdrawStatus()
    data class Error(val message: String?) : WithdrawStatus()
}

data class WithdrawalDetailsForUri(
    val amount: Amount,
    val defaultExchangeBaseUrl: String?,
    val possibleExchanges: List<ExchangeItem>
)

data class WithdrawalDetails(
    val tosAccepted: Boolean,
    val amountRaw: Amount,
    val amountEffective: Amount
)

class WithdrawManager(
    private val walletBackendApi: WalletBackendApi,
    private val mapper: ObjectMapper
) {

    val withdrawStatus = MutableLiveData<WithdrawStatus>()
    val testWithdrawalInProgress = MutableLiveData(false)

    var exchangeFees: ExchangeFees? = null
        private set

    fun withdrawTestkudos() {
        testWithdrawalInProgress.value = true
        walletBackendApi.sendRequest("withdrawTestkudos") { _, _ ->
            testWithdrawalInProgress.postValue(false)
        }
    }

    fun getWithdrawalDetails(uri: String) {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        val args = JSONObject().apply {
            put("talerWithdrawUri", uri)
        }
        walletBackendApi.sendRequest("getWithdrawalDetailsForUri", args) { isError, result ->
            if (isError) {
                handleError("getWithdrawalDetailsForUri", result)
                return@sendRequest
            }
            val details: WithdrawalDetailsForUri = mapper.readValue(result.toString())
            if (details.defaultExchangeBaseUrl == null) {
                // TODO go to exchange selection screen instead
                val chosenExchange = details.possibleExchanges[0].exchangeBaseUrl
                getWithdrawalDetails(chosenExchange, details.amount, uri)
            } else {
                getWithdrawalDetails(details.defaultExchangeBaseUrl, details.amount, uri)
            }
        }
    }

    fun getWithdrawalDetails(exchangeBaseUrl: String, amount: Amount, uri: String? = null) {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        val args = JSONObject().apply {
            put("exchangeBaseUrl", exchangeBaseUrl)
            put("amount", amount.toJSONString())
        }
        walletBackendApi.sendRequest("getWithdrawalDetailsForAmount", args) { isError, result ->
            if (isError) {
                handleError("getWithdrawalDetailsForAmount", result)
                return@sendRequest
            }
            val details: WithdrawalDetails = mapper.readValue(result.toString())
            if (details.tosAccepted)
                withdrawStatus.value = ReceivedDetails(
                    talerWithdrawUri = uri,
                    exchangeBaseUrl = exchangeBaseUrl,
                    amountRaw = details.amountRaw,
                    amountEffective = details.amountEffective
                )
            else getExchangeTos(exchangeBaseUrl, details, uri)
        }
    }

    private fun getExchangeTos(exchangeBaseUrl: String, details: WithdrawalDetails, uri: String?) {
        val args = JSONObject().apply {
            put("exchangeBaseUrl", exchangeBaseUrl)
        }
        walletBackendApi.sendRequest("getExchangeTos", args) { isError, result ->
            if (isError) {
                handleError("getExchangeTos", result)
                return@sendRequest
            }
            withdrawStatus.value = WithdrawStatus.TosReviewRequired(
                talerWithdrawUri = uri,
                exchangeBaseUrl = exchangeBaseUrl,
                amountRaw = details.amountRaw,
                amountEffective = details.amountEffective,
                tosText = result.getString("tos"),
                tosEtag = result.getString("currentEtag")
            )
        }
    }

    /**
     * Accept the currently displayed terms of service.
     */
    fun acceptCurrentTermsOfService() {
        val s = withdrawStatus.value as WithdrawStatus.TosReviewRequired
        val args = JSONObject().apply {
            put("exchangeBaseUrl", s.exchangeBaseUrl)
            put("etag", s.tosEtag)
        }
        walletBackendApi.sendRequest("setExchangeTosAccepted", args) { isError, result ->
            if (isError) {
                handleError("setExchangeTosAccepted", result)
                return@sendRequest
            }
            withdrawStatus.value = ReceivedDetails(
                talerWithdrawUri = s.talerWithdrawUri,
                exchangeBaseUrl = s.exchangeBaseUrl,
                amountRaw = s.amountRaw,
                amountEffective = s.amountEffective
            )
        }
    }

    @UiThread
    fun acceptWithdrawal() {
        val status = withdrawStatus.value as ReceivedDetails

        val operation = if (status.talerWithdrawUri == null)
            "acceptManualWithdrawal" else "acceptBankIntegratedWithdrawal"
        val args = JSONObject().apply {
            put("exchangeBaseUrl", status.exchangeBaseUrl)
            if (status.talerWithdrawUri == null) {
                put("amount", status.amountRaw)
            } else {
                put("talerWithdrawUri", status.talerWithdrawUri)
            }
        }
        withdrawStatus.value = WithdrawStatus.Withdrawing
        walletBackendApi.sendRequest(operation, args) { isError, result ->
            if (isError) {
                handleError(operation, result)
                return@sendRequest
            }
            withdrawStatus.value = WithdrawStatus.Success(status.amountRaw.currency)
        }
    }

    @UiThread
    private fun handleError(operation: String, result: JSONObject) {
        Log.e(TAG, "Error $operation ${result.toString(2)}")
        withdrawStatus.value = WithdrawStatus.Error(getErrorString(result))
    }

}
