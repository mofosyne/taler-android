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

package net.taler.cashier

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.taler.cashier.HttpHelper.makeJsonGetRequest
import net.taler.cashier.config.ConfigManager
import net.taler.cashier.withdraw.WithdrawManager
import net.taler.common.isOnline
import net.taler.lib.common.Amount
import net.taler.lib.common.AmountParserException

private val TAG = MainViewModel::class.java.simpleName

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val httpClient = HttpClient(OkHttp) {
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
    val configManager = ConfigManager(app, viewModelScope, httpClient)

    private val mBalance = MutableLiveData<BalanceResult>()
    val balance: LiveData<BalanceResult> = mBalance

    internal val withdrawManager = WithdrawManager(app, this)

    fun getBalance() = viewModelScope.launch(Dispatchers.IO) {
        check(configManager.hasConfig()) { "No config to get balance" }
        val config = configManager.config
        val url = "${config.bankUrl}/accounts/${config.username}/balance"
        Log.d(TAG, "Checking balance at $url")
        val result = when (val response = makeJsonGetRequest(url, config)) {
            is HttpJsonResult.Success -> {
                try {
                    val balance = response.json.getString("balance")
                    val positive = when (val creditDebitIndicator =
                        response.json.getString("credit_debit_indicator")) {
                        "credit" -> true
                        "debit" -> false
                        else -> throw AmountParserException("Unexpected credit_debit_indicator: $creditDebitIndicator")
                    }
                    BalanceResult.Success(SignedAmount(positive, Amount.fromJSONString(balance)))
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing balance", e)
                    BalanceResult.Error("Invalid amount:\n${response.json.toString(2)}")
                }
            }
            is HttpJsonResult.Error -> {
                if (app.isOnline()) BalanceResult.Error(response.msg)
                else BalanceResult.Offline
            }
        }
        mBalance.postValue(result)
    }

    fun lock() {
        configManager.lock()
    }

}
