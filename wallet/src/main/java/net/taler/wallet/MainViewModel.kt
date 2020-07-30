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
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.common.Amount
import net.taler.common.AmountMixin
import net.taler.common.Event
import net.taler.common.assertUiThread
import net.taler.common.toEvent
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.balances.BalanceItem
import net.taler.wallet.exchanges.ExchangeManager
import net.taler.wallet.payment.PaymentManager
import net.taler.wallet.pending.PendingOperationsManager
import net.taler.wallet.refund.RefundManager
import net.taler.wallet.transactions.TransactionManager
import net.taler.wallet.withdraw.WithdrawManager
import org.json.JSONObject
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.MINUTES
import kotlin.random.Random

const val TAG = "taler-wallet"

private val transactionNotifications = listOf(
    "proposal-accepted",
    "refresh-revealed",
    "withdraw-group-finished"
)

class MainViewModel(val app: Application) : AndroidViewModel(app) {

    private val mBalances = MutableLiveData<List<BalanceItem>>()
    val balances: LiveData<List<BalanceItem>> = mBalances.distinctUntilChanged()

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
            }
        }
    }

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addMixIn(Amount::class.java, AmountMixin::class.java)
        .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)

    val withdrawManager = WithdrawManager(walletBackendApi, mapper)
    val paymentManager = PaymentManager(walletBackendApi, mapper)
    val pendingOperationsManager: PendingOperationsManager =
        PendingOperationsManager(walletBackendApi)
    val transactionManager: TransactionManager =
        TransactionManager(walletBackendApi, viewModelScope, mapper)
    val refundManager = RefundManager(walletBackendApi)
    val exchangeManager: ExchangeManager = ExchangeManager(walletBackendApi, mapper)

    private val mTransactionsEvent = MutableLiveData<Event<String>>()
    val transactionsEvent: LiveData<Event<String>> = mTransactionsEvent

    private val mLastBackup = MutableLiveData(
        // fake backup time until we actually do backup
        System.currentTimeMillis() -
                Random.nextLong(MINUTES.toMillis(5), DAYS.toMillis(2))
    )
    val lastBackup: LiveData<Long> = mLastBackup

    override fun onCleared() {
        walletBackendApi.destroy()
        super.onCleared()
    }

    @UiThread
    fun loadBalances() {
        showProgressBar.value = true
        walletBackendApi.sendRequest("getBalances") { isError, result ->
            if (isError) {
                Log.e(TAG, "Error retrieving balances: ${result.toString(2)}")
                return@sendRequest
            }
            mBalances.value = mapper.readValue(result.getString("balances"))
            showProgressBar.value = false
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
        walletBackendApi.sendRequest("reset")
        withdrawManager.testWithdrawalInProgress.value = false
        mBalances.value = emptyList()
    }

    fun startTunnel() {
        walletBackendApi.sendRequest("startTunnel")
    }

    fun stopTunnel() {
        walletBackendApi.sendRequest("stopTunnel")
    }

    fun tunnelResponse(resp: String) {
        val respJson = JSONObject(resp)
        walletBackendApi.sendRequest("tunnelResponse", respJson)
    }

}
