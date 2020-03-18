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
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.history.HistoryManager
import net.taler.wallet.payment.PaymentManager
import net.taler.wallet.pending.PendingOperationsManager
import net.taler.wallet.withdraw.WithdrawManager
import org.json.JSONObject

const val TAG = "taler-wallet"

data class BalanceItem(val available: Amount, val pendingIncoming: Amount)

class WalletViewModel(val app: Application) : AndroidViewModel(app) {

    private val mBalances = MutableLiveData<List<BalanceItem>>()
    val balances: LiveData<List<BalanceItem>> = mBalances.distinctUntilChanged()

    val devMode = MutableLiveData(BuildConfig.DEBUG)
    val showProgressBar = MutableLiveData<Boolean>()

    private var activeGetBalance = 0

    private val walletBackendApi = WalletBackendApi(app, {
        activeGetBalance = 0
        loadBalances()
        pendingOperationsManager.getPending()
    }) {
        Log.i(TAG, "Received notification from wallet-core")
        loadBalances()
        pendingOperationsManager.getPending()
    }

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    val withdrawManager = WithdrawManager(walletBackendApi)
    val paymentManager = PaymentManager(walletBackendApi, mapper)
    val pendingOperationsManager: PendingOperationsManager =
        PendingOperationsManager(walletBackendApi)
    val historyManager = HistoryManager(walletBackendApi, mapper)

    override fun onCleared() {
        walletBackendApi.destroy()
        super.onCleared()
    }

    @UiThread
    fun loadBalances() {
        if (activeGetBalance > 0) {
            return
        }
        activeGetBalance++
        showProgressBar.value = true
        walletBackendApi.sendRequest("getBalances", null) { isError, result ->
            activeGetBalance--
            if (isError) {
                return@sendRequest
            }
            val balanceList = mutableListOf<BalanceItem>()
            val byCurrency = result.getJSONObject("byCurrency")
            val currencyList = byCurrency.keys().asSequence().toList().sorted()
            for (currency in currencyList) {
                val jsonAmount = byCurrency.getJSONObject(currency)
                    .getJSONObject("available")
                val amount = Amount.fromJson(jsonAmount)
                val jsonAmountIncoming = byCurrency.getJSONObject(currency)
                    .getJSONObject("pendingIncoming")
                val amountIncoming = Amount.fromJson(jsonAmountIncoming)
                balanceList.add(BalanceItem(amount, amountIncoming))
            }
            mBalances.postValue(balanceList)
            showProgressBar.postValue(false)
        }
    }

    @UiThread
    fun dangerouslyReset() {
        walletBackendApi.sendRequest("reset", null)
        withdrawManager.testWithdrawalInProgress.value = false
        mBalances.value = emptyList()
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
