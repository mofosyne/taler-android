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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.lib.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeFees
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails

sealed class WithdrawStatus {
    data class Loading(val talerWithdrawUri: String? = null) : WithdrawStatus()
    data class NeedsExchange(val exchangeSelection: Event<ExchangeSelection>) : WithdrawStatus()

    data class TosReviewRequired(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val tosText: String,
        val tosEtag: String,
        val showImmediately: Event<Boolean>,
    ) : WithdrawStatus()

    data class ReceivedDetails(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
    ) : WithdrawStatus()

    object Withdrawing : WithdrawStatus()
    data class Success(val currency: String) : WithdrawStatus()
    data class Error(val message: String?) : WithdrawStatus()
}

sealed class WithdrawTestStatus {
    object Withdrawing : WithdrawTestStatus()
    object Success : WithdrawTestStatus()
    data class Error(val message: String) : WithdrawTestStatus()
}

@Serializable
data class WithdrawalDetailsForUri(
    val amount: Amount,
    val defaultExchangeBaseUrl: String?,
    val possibleExchanges: List<ExchangeItem>,
)

@Serializable
data class WithdrawalDetails(
    val tosAccepted: Boolean,
    val amountRaw: Amount,
    val amountEffective: Amount,
)

data class ExchangeSelection(
    val amount: Amount,
    val talerWithdrawUri: String,
)

class WithdrawManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    val withdrawStatus = MutableLiveData<WithdrawStatus>()
    val testWithdrawalStatus = MutableLiveData<WithdrawTestStatus>()

    private val _exchangeSelection = MutableLiveData<Event<ExchangeSelection>>()
    val exchangeSelection: LiveData<Event<ExchangeSelection>> = _exchangeSelection
    var exchangeFees: ExchangeFees? = null
        private set

    fun withdrawTestkudos() = scope.launch {
        testWithdrawalStatus.value = WithdrawTestStatus.Withdrawing
        api.request<Unit>("withdrawTestkudos").onError {
            testWithdrawalStatus.value = WithdrawTestStatus.Error(it.userFacingMsg)
        }.onSuccess {
            testWithdrawalStatus.value = WithdrawTestStatus.Success
        }
    }

    @UiThread
    fun selectExchange(selection: ExchangeSelection) {
        _exchangeSelection.value = selection.toEvent()
    }

    fun getWithdrawalDetails(uri: String) = scope.launch {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        api.request("getWithdrawalDetailsForUri", WithdrawalDetailsForUri.serializer()) {
            put("talerWithdrawUri", uri)
        }.onError { error ->
            handleError("getWithdrawalDetailsForUri", error)
        }.onSuccess { details ->
            if (details.defaultExchangeBaseUrl == null) {
                val exchangeSelection = ExchangeSelection(details.amount, uri)
                withdrawStatus.value = WithdrawStatus.NeedsExchange(exchangeSelection.toEvent())
            } else {
                getWithdrawalDetails(details.defaultExchangeBaseUrl, details.amount, false, uri)
            }
        }
    }

    fun getWithdrawalDetails(
        exchangeBaseUrl: String,
        amount: Amount,
        showTosImmediately: Boolean = false,
        uri: String? = null,
    ) = scope.launch {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        api.request("getWithdrawalDetailsForAmount", WithdrawalDetails.serializer()) {
            put("exchangeBaseUrl", exchangeBaseUrl)
            put("amount", amount.toJSONString())
        }.onError { error ->
            handleError("getWithdrawalDetailsForAmount", error)
        }.onSuccess { details ->
            if (details.tosAccepted) {
                withdrawStatus.value = ReceivedDetails(
                    talerWithdrawUri = uri,
                    exchangeBaseUrl = exchangeBaseUrl,
                    amountRaw = details.amountRaw,
                    amountEffective = details.amountEffective,
                )
            } else getExchangeTos(exchangeBaseUrl, details, showTosImmediately, uri)
        }
    }

    private fun getExchangeTos(
        exchangeBaseUrl: String,
        details: WithdrawalDetails,
        showImmediately: Boolean,
        uri: String?,
    ) = scope.launch {
        api.request("getExchangeTos", TosResponse.serializer()) {
            put("exchangeBaseUrl", exchangeBaseUrl)
        }.onError {
            handleError("getExchangeTos", it)
        }.onSuccess {
            withdrawStatus.value = WithdrawStatus.TosReviewRequired(
                talerWithdrawUri = uri,
                exchangeBaseUrl = exchangeBaseUrl,
                amountRaw = details.amountRaw,
                amountEffective = details.amountEffective,
                tosText = it.content,
                tosEtag = it.currentEtag,
                showImmediately = showImmediately.toEvent(),
            )
        }
    }

    /**
     * Accept the currently displayed terms of service.
     */
    fun acceptCurrentTermsOfService() = scope.launch {
        val s = withdrawStatus.value as WithdrawStatus.TosReviewRequired
        api.request<Unit>("setExchangeTosAccepted") {
            put("exchangeBaseUrl", s.exchangeBaseUrl)
            put("etag", s.tosEtag)
        }.onError {
            handleError("setExchangeTosAccepted", it)
        }.onSuccess {
            withdrawStatus.value = ReceivedDetails(
                talerWithdrawUri = s.talerWithdrawUri,
                exchangeBaseUrl = s.exchangeBaseUrl,
                amountRaw = s.amountRaw,
                amountEffective = s.amountEffective,
            )
        }
    }

    @UiThread
    fun acceptWithdrawal() = scope.launch {
        val status = withdrawStatus.value as ReceivedDetails
        val operation = if (status.talerWithdrawUri == null) {
            "acceptManualWithdrawal"
        } else {
            "acceptBankIntegratedWithdrawal"
        }
        withdrawStatus.value = WithdrawStatus.Withdrawing

        api.request<Unit>(operation) {
            put("exchangeBaseUrl", status.exchangeBaseUrl)
            if (status.talerWithdrawUri == null) {
                put("amount", status.amountRaw.toJSONString())
            } else {
                put("talerWithdrawUri", status.talerWithdrawUri)
            }
        }.onError {
            handleError(operation, it)
        }.onSuccess {
            withdrawStatus.value = WithdrawStatus.Success(status.amountRaw.currency)
        }
    }

    @UiThread
    private fun handleError(operation: String, error: TalerErrorInfo) {
        Log.e(TAG, "Error $operation $error")
        withdrawStatus.value = WithdrawStatus.Error(error.userFacingMsg)
    }

}
