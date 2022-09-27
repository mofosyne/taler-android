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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.taler.common.Amount
import net.taler.common.AmountParserException
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.accounts.AccountManager
import net.taler.wallet.backend.NotificationPayload
import net.taler.wallet.backend.NotificationReceiver
import net.taler.wallet.backend.VersionReceiver
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.backend.WalletCoreVersion
import net.taler.wallet.balances.BalanceItem
import net.taler.wallet.balances.BalanceResponse
import net.taler.wallet.deposit.DepositManager
import net.taler.wallet.exchanges.ExchangeManager
import net.taler.wallet.payment.PaymentManager
import net.taler.wallet.peer.PeerManager
import net.taler.wallet.pending.PendingOperationsManager
import net.taler.wallet.refund.RefundManager
import net.taler.wallet.settings.SettingsManager
import net.taler.wallet.tip.TipManager
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

class MainViewModel(
    app: Application,
) : AndroidViewModel(app), VersionReceiver, NotificationReceiver {

    private val mBalances = MutableLiveData<List<BalanceItem>>()
    val balances: LiveData<List<BalanceItem>> = mBalances.distinctUntilChanged()

    val devMode = MutableLiveData(BuildConfig.DEBUG)
    val showProgressBar = MutableLiveData<Boolean>()
    var exchangeVersion: String? = null
        private set
    var merchantVersion: String? = null
        private set

    private val api = WalletBackendApi(app, this, this)

    val withdrawManager = WithdrawManager(api, viewModelScope)
    val tipManager = TipManager(api, viewModelScope)
    val paymentManager = PaymentManager(api, viewModelScope)
    val pendingOperationsManager: PendingOperationsManager =
        PendingOperationsManager(api, viewModelScope)
    val transactionManager: TransactionManager = TransactionManager(api, viewModelScope)
    val refundManager = RefundManager(api, viewModelScope)
    val exchangeManager: ExchangeManager = ExchangeManager(api, viewModelScope)
    val peerManager: PeerManager = PeerManager(api, exchangeManager, viewModelScope)
    val settingsManager: SettingsManager = SettingsManager(app.applicationContext, viewModelScope)
    val accountManager: AccountManager = AccountManager(api, viewModelScope)
    val depositManager: DepositManager = DepositManager(api, viewModelScope)

    private val mTransactionsEvent = MutableLiveData<Event<String>>()
    val transactionsEvent: LiveData<Event<String>> = mTransactionsEvent

    private val mScanCodeEvent = MutableLiveData<Event<Boolean>>()
    val scanCodeEvent: LiveData<Event<Boolean>> = mScanCodeEvent

    private val mLastBackup = MutableLiveData(
        // fake backup time until we actually do backup
        System.currentTimeMillis() -
                Random.nextLong(MINUTES.toMillis(5), DAYS.toMillis(2))
    )
    val lastBackup: LiveData<Long> = mLastBackup

    override fun onVersionReceived(versionInfo: WalletCoreVersion) {
        exchangeVersion = versionInfo.exchange
        merchantVersion = versionInfo.merchant
    }

    override fun onNotificationReceived(payload: NotificationPayload) {
        if (payload.type == "waiting-for-retry") return // ignore ping)
        Log.i(TAG, "Received notification from wallet-core: $payload")

        loadBalances()
        if (payload.type in transactionNotifications) viewModelScope.launch(Dispatchers.Main) {
            // TODO notification API should give us a currency to update
            // update currently selected transaction list
            transactionManager.loadTransactions()
        }
        // refresh pending ops and history with each notification
        if (devMode.value == true) {
            pendingOperationsManager.getPending()
        }
    }

    @UiThread
    fun loadBalances(): Job = viewModelScope.launch {
        showProgressBar.value = true
        val response = api.request("getBalances", BalanceResponse.serializer())
        showProgressBar.value = false
        response.onError {
            // TODO expose in UI
            Log.e(TAG, "Error retrieving balances: $it")
        }
        response.onSuccess {
            mBalances.value = it.balances
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
    fun getCurrencies(): List<String> {
        return balances.value?.map { balanceItem ->
            balanceItem.currency
        } ?: emptyList()
    }

    @UiThread
    fun createAmount(amountText: String, currency: String): AmountResult {
        val amount = try {
            Amount.fromString(currency, amountText)
        } catch (e: AmountParserException) {
            return AmountResult.InvalidAmount
        }
        if (hasSufficientBalance(amount)) return AmountResult.Success(amount)
        return AmountResult.InsufficientBalance
    }

    @UiThread
    fun hasSufficientBalance(amount: Amount): Boolean {
        balances.value?.forEach { balanceItem ->
            if (balanceItem.currency == amount.currency) {
                return balanceItem.available >= amount
            }
        }
        return false
    }

    @UiThread
    fun dangerouslyReset() {
        viewModelScope.launch {
            api.sendRequest("reset")
        }
        withdrawManager.testWithdrawalStatus.value = null
        mBalances.value = emptyList()
    }

    fun startTunnel() {
        viewModelScope.launch {
            api.sendRequest("startTunnel")
        }
    }

    fun stopTunnel() {
        viewModelScope.launch {
            api.sendRequest("stopTunnel")
        }
    }

    fun tunnelResponse(resp: String) {
        viewModelScope.launch {
            api.sendRequest("tunnelResponse", JSONObject(resp))
        }
    }

    @UiThread
    fun scanCode() {
        mScanCodeEvent.value = true.toEvent()
    }

}

sealed class AmountResult {
    class Success(val amount: Amount) : AmountResult()
    object InsufficientBalance : AmountResult()
    object InvalidAmount : AmountResult()
}
