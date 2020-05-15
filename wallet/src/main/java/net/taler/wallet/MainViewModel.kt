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

package net.taler.wallet

import android.app.Application
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.taler.common.Amount
import net.taler.common.Event
import net.taler.common.assertUiThread
import net.taler.common.toEvent
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.balances.BalanceItem
import net.taler.wallet.history.DevHistoryManager
import net.taler.wallet.payment.PaymentManager
import net.taler.wallet.pending.PendingOperationsManager
import net.taler.wallet.refund.RefundManager
import net.taler.wallet.transactions.TransactionManager
import net.taler.wallet.withdraw.WithdrawManager
import org.json.JSONObject

const val TAG = "taler-wallet"

private val transactionNotifications = listOf(
    "proposal-accepted",
    "refresh-revealed",
    "withdraw-group-finished"
)

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val mBalances = MutableLiveData<Map<String, BalanceItem>>()
    val balances: LiveData<Map<String, BalanceItem>> = mBalances.distinctUntilChanged()

    val devMode = MutableLiveData(BuildConfig.DEBUG)
    val showProgressBar = MutableLiveData<Boolean>()
    var exchangeVersion: String? = null
        private set
    var merchantVersion: String? = null
        private set

    private val walletBackendApi = WalletBackendApi(app, {
        // nothing to do when we connect, balance will be requested by BalanceFragment in onStart()
    }) { payload ->
        if (payload.optString("operation") == "init") {
            val result = payload.getJSONObject("result")
            val versions = result.getJSONObject("supported_protocol_versions")
            exchangeVersion = versions.getString("exchange")
            merchantVersion = versions.getString("merchant")
        } else if (payload.getString("type") != "waiting-for-retry") {  // ignore ping
            Log.i(TAG, "Received notification from wallet-core: ${payload.toString(2)}")
            loadBalances()
            if (payload.optString("type") in transactionNotifications) {
                assertUiThread()
                // TODO notification API should give us a currency to update
                // update currently selected transaction list
                transactionManager.loadTransactions()
            }
            // refresh pending ops and history with each notification
            if (devMode.value == true) {
                pendingOperationsManager.getPending()
                historyManager.loadHistory()
            }
        }
    }

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    val withdrawManager = WithdrawManager(walletBackendApi)
    val paymentManager = PaymentManager(walletBackendApi, mapper)
    val pendingOperationsManager: PendingOperationsManager =
        PendingOperationsManager(walletBackendApi)
    val historyManager: DevHistoryManager =
        DevHistoryManager(walletBackendApi, viewModelScope, mapper)
    val transactionManager: TransactionManager =
        TransactionManager(walletBackendApi, viewModelScope, mapper)
    val refundManager = RefundManager(walletBackendApi)

    private val mTransactionsEvent = MutableLiveData<Event<String>>()
    val transactionsEvent: LiveData<Event<String>> = mTransactionsEvent

    override fun onCleared() {
        walletBackendApi.destroy()
        super.onCleared()
    }

    @UiThread
    fun loadBalances() {
        showProgressBar.value = true
        walletBackendApi.sendRequest("getBalances", null) { isError, result ->
            if (isError) {
                Log.e(TAG, "Error retrieving balances: ${result.toString(2)}")
                return@sendRequest
            }
            val balanceMap = HashMap<String, BalanceItem>()
            val byCurrency = result.getJSONObject("byCurrency")
            val currencyList = byCurrency.keys().asSequence().toList().sorted()
            for (currency in currencyList) {
                val jsonAmount = byCurrency.getJSONObject(currency)
                    .getJSONObject("available")
                val amount = Amount.fromJsonObject(jsonAmount)
                val jsonAmountIncoming = byCurrency.getJSONObject(currency)
                    .getJSONObject("pendingIncoming")
                val amountIncoming = Amount.fromJsonObject(jsonAmountIncoming)
                val hasPending = transactionManager.hasPending(currency)
                balanceMap[currency] = BalanceItem(amount, amountIncoming, hasPending)
            }
            mBalances.postValue(balanceMap)
            showProgressBar.postValue(false)
        }
    }

    /**
     * Navigates to the given currency's transaction list, when [MainFragment] is shown.
     */
    @UiThread
    fun showTransactions(currency: String) {
        mTransactionsEvent.value = currency.toEvent()
    }

    @UiThread
    fun dangerouslyReset() {
        walletBackendApi.sendRequest("reset", null)
        withdrawManager.testWithdrawalInProgress.value = false
        mBalances.value = emptyMap()
    }

    fun startTunnel() {
        walletBackendApi.sendRequest("startTunnel", null)
    }

    fun stopTunnel() {
        walletBackendApi.sendRequest("stopTunnel", null)
    }

    fun tunnelResponse(resp: String) {
        val respJson = JSONObject(resp)
        walletBackendApi.sendRequest("tunnelResponse", respJson)
    }

}
