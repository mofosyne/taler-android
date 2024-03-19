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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
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
import net.taler.wallet.balances.BalanceManager
import net.taler.wallet.balances.ScopeInfo
import net.taler.wallet.deposit.DepositManager
import net.taler.wallet.exchanges.ExchangeManager
import net.taler.wallet.payment.PaymentManager
import net.taler.wallet.peer.PeerManager
import net.taler.wallet.refund.RefundManager
import net.taler.wallet.settings.SettingsManager
import net.taler.wallet.transactions.TransactionManager
import net.taler.wallet.withdraw.WithdrawManager
import org.json.JSONObject

const val TAG = "taler-wallet"

private val transactionNotifications = listOf(
    "transaction-state-transition",
)

class MainViewModel(
    app: Application,
) : AndroidViewModel(app), VersionReceiver, NotificationReceiver {

    val devMode = MutableLiveData(BuildConfig.DEBUG)
    val showProgressBar = MutableLiveData<Boolean>()
    var walletVersion: String? = null
        private set
    var walletVersionHash: String? = null
        private set
    var exchangeVersion: String? = null
        private set
    var merchantVersion: String? = null
        private set

    private val api = WalletBackendApi(app, this, this)

    val networkManager = NetworkManager(app.applicationContext)
    val withdrawManager = WithdrawManager(api, viewModelScope)
    val paymentManager = PaymentManager(api, viewModelScope)
    val transactionManager: TransactionManager = TransactionManager(api, viewModelScope)
    val refundManager = RefundManager(api, viewModelScope)
    val balanceManager = BalanceManager(api, viewModelScope)
    val exchangeManager: ExchangeManager = ExchangeManager(api, viewModelScope)
    val peerManager: PeerManager = PeerManager(api, exchangeManager, viewModelScope)
    val settingsManager: SettingsManager = SettingsManager(app.applicationContext, api, viewModelScope)
    val accountManager: AccountManager = AccountManager(api, viewModelScope)
    val depositManager: DepositManager = DepositManager(api, viewModelScope)

    private val mTransactionsEvent = MutableLiveData<Event<ScopeInfo>>()
    val transactionsEvent: LiveData<Event<ScopeInfo>> = mTransactionsEvent

    private val mScanCodeEvent = MutableLiveData<Event<Boolean>>()
    val scanCodeEvent: LiveData<Event<Boolean>> = mScanCodeEvent

    override fun onVersionReceived(versionInfo: WalletCoreVersion) {
        walletVersion = versionInfo.implementationSemver
        walletVersionHash = versionInfo.implementationGitHash
        exchangeVersion = versionInfo.exchange
        merchantVersion = versionInfo.merchant
    }

    override fun onNotificationReceived(payload: NotificationPayload) {
        if (payload.type == "waiting-for-retry") return // ignore ping)
        Log.i(TAG, "Received notification from wallet-core: $payload")

        // Only update balances when we're told they changed
        if (payload.type == "balance-change") viewModelScope.launch(Dispatchers.Main) {
            balanceManager.loadBalances()
        }

        if (payload.type in transactionNotifications) viewModelScope.launch(Dispatchers.Main) {
            // TODO notification API should give us a currency to update
            // update currently selected transaction list
            transactionManager.loadTransactions()
        }
    }

    /**
     * Navigates to the given scope info's transaction list, when [MainFragment] is shown.
     */
    @UiThread
    fun showTransactions(scopeInfo: ScopeInfo) {
        mTransactionsEvent.value = scopeInfo.toEvent()
    }

    @UiThread
    fun getCurrencies() = balanceManager.balances.value?.map { balanceItem ->
        balanceItem.currency
    } ?: emptyList()

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
        balanceManager.balances.value?.forEach { balanceItem ->
            if (balanceItem.currency == amount.currency) {
                return balanceItem.available >= amount
            }
        }
        return false
    }

    @UiThread
    fun dangerouslyReset() {
        withdrawManager.testWithdrawalStatus.value = null
        balanceManager.resetBalances()
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

    fun runIntegrationTest() {
        viewModelScope.launch {
            api.request<Unit>("runIntegrationTestV2") {
                put("amountToWithdraw", "KUDOS:42")
                put("amountToSpend", "KUDOS:23")
                put("corebankApiBaseUrl", "https://bank.demo.taler.net/")
                put("exchangeBaseUrl", "https://exchange.demo.taler.net/")
                put("merchantBaseUrl", "https://backend.demo.taler.net/")
                put("merchantAuthToken", "secret-token:sandbox")
            }
        }
    }

}

sealed class AmountResult {
    class Success(val amount: Amount) : AmountResult()
    object InsufficientBalance : AmountResult()
    object InvalidAmount : AmountResult()
}
