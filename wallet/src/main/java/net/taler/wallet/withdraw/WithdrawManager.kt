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

import android.net.Uri
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Amount
import net.taler.common.Bech32
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.exchanges.ExchangeFees
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.transactions.WithdrawalExchangeAccountDetails
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails

sealed class WithdrawStatus {
    data class Loading(val talerWithdrawUri: String? = null) : WithdrawStatus()

    data class NeedsExchange(
        val talerWithdrawUri: String,
        val amount: Amount,
        val possibleExchanges: List<ExchangeItem>,
    ) : WithdrawStatus()

    data class TosReviewRequired(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val withdrawalAccountList: List<WithdrawalExchangeAccountDetails>,
        val ageRestrictionOptions: List<Int>? = null,
        val tosText: String,
        val tosEtag: String,
        val showImmediately: Event<Boolean>,
        val possibleExchanges: List<ExchangeItem> = emptyList(),
    ) : WithdrawStatus()

    data class ReceivedDetails(
        val talerWithdrawUri: String? = null,
        val exchangeBaseUrl: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val withdrawalAccountList: List<WithdrawalExchangeAccountDetails>,
        val ageRestrictionOptions: List<Int>? = null,
        val possibleExchanges: List<ExchangeItem> = emptyList(),
    ) : WithdrawStatus()

    data object Withdrawing : WithdrawStatus()

    data class Success(val currency: String, val transactionId: String) : WithdrawStatus()

    class ManualTransferRequired(
        val transactionId: String?,
        val transactionAmountRaw: Amount,
        val transactionAmountEffective: Amount,
        val exchangeBaseUrl: String,
        val withdrawalTransfers: List<TransferData>,
    ) : WithdrawStatus()

    data class Error(val message: String?) : WithdrawStatus()
}

sealed class TransferData {
    abstract val subject: String
    abstract val amountRaw: Amount
    abstract val amountEffective: Amount
    abstract val withdrawalAccount: WithdrawalExchangeAccountDetails

    val currency get() = withdrawalAccount.transferAmount?.currency

    data class Taler(
        override val subject: String,
        override val amountRaw: Amount,
        override val amountEffective: Amount,
        override val withdrawalAccount: WithdrawalExchangeAccountDetails,
        val receiverName: String? = null,
        val bankUrl: String,
        val account: String,
    ): TransferData()

    data class IBAN(
        override val subject: String,
        override val amountRaw: Amount,
        override val amountEffective: Amount,
        override val withdrawalAccount: WithdrawalExchangeAccountDetails,
        val receiverName: String? = null,
        val iban: String,
    ): TransferData()

    data class Bitcoin(
        override val subject: String,
        override val amountRaw: Amount,
        override val amountEffective: Amount,
        override val withdrawalAccount: WithdrawalExchangeAccountDetails,
        val account: String,
        val segwitAddresses: List<String>,
    ): TransferData()
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
data class WithdrawExchangeResponse(
    val exchangeBaseUrl: String,
    val amount: Amount? = null,
)

@Serializable
data class ManualWithdrawalDetails(
    val tosAccepted: Boolean,
    val amountRaw: Amount,
    val amountEffective: Amount,
    val withdrawalAccountsList: List<WithdrawalExchangeAccountDetails>,
    val ageRestrictionOptions: List<Int>? = null,
)

@Serializable
data class AcceptWithdrawalResponse(
    val transactionId: String,
)

@Serializable
data class AcceptManualWithdrawalResponse(
    val reservePub: String,
    val withdrawalAccountsList: List<WithdrawalExchangeAccountDetails>,
    val transactionId: String,
)

class WithdrawManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    val withdrawStatus = MutableLiveData<WithdrawStatus>()
    val testWithdrawalStatus = MutableLiveData<WithdrawTestStatus>()

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

    fun getWithdrawalDetails(uri: String) = scope.launch {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        api.request("getWithdrawalDetailsForUri", WithdrawalDetailsForUri.serializer()) {
            put("talerWithdrawUri", uri)
        }.onError { error ->
            handleError("getWithdrawalDetailsForUri", error)
        }.onSuccess { details ->
            if (details.defaultExchangeBaseUrl == null) {
                withdrawStatus.value = WithdrawStatus.NeedsExchange(
                    talerWithdrawUri = uri,
                    amount = details.amount,
                    possibleExchanges = details.possibleExchanges,
                )
            } else getWithdrawalDetails(
                exchangeBaseUrl = details.defaultExchangeBaseUrl,
                amount = details.amount,
                showTosImmediately = false,
                uri = uri,
                possibleExchanges = details.possibleExchanges,
            )
        }
    }

    fun getWithdrawalDetails(
        exchangeBaseUrl: String,
        amount: Amount,
        showTosImmediately: Boolean = false,
        uri: String? = null,
        possibleExchanges: List<ExchangeItem> = emptyList(),
    ) = scope.launch {
        withdrawStatus.value = WithdrawStatus.Loading(uri)
        api.request("getWithdrawalDetailsForAmount", ManualWithdrawalDetails.serializer()) {
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
                    withdrawalAccountList = details.withdrawalAccountsList,
                    ageRestrictionOptions = details.ageRestrictionOptions,
                    possibleExchanges = possibleExchanges,
                )
            } else getExchangeTos(exchangeBaseUrl, details, showTosImmediately, uri, possibleExchanges)
        }
    }

    @WorkerThread
    suspend fun prepareManualWithdrawal(uri: String): WithdrawExchangeResponse? {
        withdrawStatus.postValue(WithdrawStatus.Loading(uri))
        var response: WithdrawExchangeResponse? = null
        api.request("prepareWithdrawExchange", WithdrawExchangeResponse.serializer()) {
            put("talerUri", uri)
        }.onError {
            handleError("prepareWithdrawExchange", it)
        }.onSuccess {
            response = it
        }
        return response
    }

    private fun getExchangeTos(
        exchangeBaseUrl: String,
        details: ManualWithdrawalDetails,
        showImmediately: Boolean,
        uri: String?,
        possibleExchanges: List<ExchangeItem>,
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
                withdrawalAccountList = details.withdrawalAccountsList,
                ageRestrictionOptions = details.ageRestrictionOptions,
                tosText = it.content,
                tosEtag = it.currentEtag,
                showImmediately = showImmediately.toEvent(),
                possibleExchanges = possibleExchanges,
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
                withdrawalAccountList = s.withdrawalAccountList,
                ageRestrictionOptions = s.ageRestrictionOptions,
                possibleExchanges = s.possibleExchanges,
            )
        }
    }

    @UiThread
    fun acceptWithdrawal(restrictAge: Int? = null) = scope.launch {
        val status = withdrawStatus.value as ReceivedDetails
        withdrawStatus.value = WithdrawStatus.Withdrawing
        if (status.talerWithdrawUri == null) {
            acceptManualWithdrawal(status, restrictAge)
        } else {
            acceptBankIntegratedWithdrawal(status, restrictAge)
        }
    }

    private suspend fun acceptBankIntegratedWithdrawal(
        status: ReceivedDetails,
        restrictAge: Int? = null,
    ) {
        api.request("acceptBankIntegratedWithdrawal", AcceptWithdrawalResponse.serializer()) {
            restrictAge?.let { put("restrictAge", restrictAge) }
            put("exchangeBaseUrl", status.exchangeBaseUrl)
            put("talerWithdrawUri", status.talerWithdrawUri)
        }.onError {
            handleError("acceptBankIntegratedWithdrawal", it)
        }.onSuccess {
            withdrawStatus.value =
                WithdrawStatus.Success(status.amountRaw.currency, it.transactionId)
        }
    }

    private suspend fun acceptManualWithdrawal(status: ReceivedDetails, restrictAge: Int? = null) {
        api.request("acceptManualWithdrawal", AcceptManualWithdrawalResponse.serializer()) {
            restrictAge?.let { put("restrictAge", restrictAge) }
            put("exchangeBaseUrl", status.exchangeBaseUrl)
            put("amount", status.amountRaw.toJSONString())
        }.onError {
            handleError("acceptManualWithdrawal", it)
        }.onSuccess { response ->
            withdrawStatus.value = createManualTransferRequired(
                status = status,
                response = response,
            )
        }
    }

    private fun handleError(operation: String, error: TalerErrorInfo) {
        Log.e(TAG, "Error $operation $error")
        withdrawStatus.postValue(WithdrawStatus.Error(error.userFacingMsg))
    }

    /**
     * A hack to be able to view bank details for manual withdrawal with the same logic.
     * Don't call this from ongoing withdrawal processes as it destroys state.
     */
    fun viewManualWithdrawal(status: WithdrawStatus.ManualTransferRequired) {
        require(status.transactionId != null) { "No transaction ID given" }
        withdrawStatus.value = status
    }

}

fun createManualTransferRequired(
    transactionId: String,
    exchangeBaseUrl: String,
    amountRaw: Amount,
    amountEffective: Amount,
    withdrawalAccountList: List<WithdrawalExchangeAccountDetails>,
) = WithdrawStatus.ManualTransferRequired(
    transactionId = transactionId,
    transactionAmountRaw = amountRaw,
    transactionAmountEffective = amountEffective,
    exchangeBaseUrl = exchangeBaseUrl,
    withdrawalTransfers = withdrawalAccountList.mapNotNull {
        val uri = Uri.parse(it.paytoUri.replace("receiver-name=", "receiver_name="))
        if ("bitcoin".equals(uri.authority, true)) {
            val msg = uri.getQueryParameter("message").orEmpty()
            val reg = "\\b([A-Z0-9]{52})\\b".toRegex().find(msg)
            val reserve = reg?.value ?: uri.getQueryParameter("subject")!!
            val segwitAddresses = Bech32.generateFakeSegwitAddress(reserve, uri.pathSegments.first())
            TransferData.Bitcoin(
                account = uri.lastPathSegment!!,
                segwitAddresses = segwitAddresses,
                subject = reserve,
                amountRaw = amountRaw,
                amountEffective = amountEffective,
                withdrawalAccount = it.copy(paytoUri = uri.toString())
            )
        } else if (uri.authority.equals("x-taler-bank", true)) {
            TransferData.Taler(
                account = uri.lastPathSegment!!,
                bankUrl = uri.pathSegments.first(),
                receiverName = uri.getQueryParameter("receiver_name"),
                subject = uri.getQueryParameter("message") ?: "Error: No message in URI",
                amountRaw = amountRaw,
                amountEffective = amountEffective,
                withdrawalAccount = it.copy(paytoUri = uri.toString()),
            )
        } else if (uri.authority.equals("iban", true)) {
            TransferData.IBAN(
                iban = uri.lastPathSegment!!,
                subject = uri.getQueryParameter("message") ?: "Error: No message in URI",
                amountRaw = amountRaw,
                amountEffective = amountEffective,
                withdrawalAccount = it.copy(paytoUri = uri.toString()),
            )
        } else null
    },
)

fun createManualTransferRequired(
    status: ReceivedDetails,
    response: AcceptManualWithdrawalResponse,
): WithdrawStatus.ManualTransferRequired = createManualTransferRequired(
    transactionId = response.transactionId,
    exchangeBaseUrl = status.exchangeBaseUrl,
    amountRaw = status.amountRaw,
    amountEffective = status.amountEffective,
    withdrawalAccountList = response.withdrawalAccountsList,
)