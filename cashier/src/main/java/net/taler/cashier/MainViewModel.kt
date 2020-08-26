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

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.cashier.HttpHelper.makeJsonGetRequest
import net.taler.cashier.withdraw.WithdrawManager
import net.taler.common.getIncompatibleStringOrNull
import net.taler.common.isOnline
import net.taler.lib.common.Amount
import net.taler.lib.common.AmountParserException
import net.taler.lib.common.Version

private val TAG = MainViewModel::class.java.simpleName

private val VERSION_BANK = Version(0, 0, 0)
private const val PREF_NAME = "net.taler.cashier.prefs"
private const val PREF_KEY_BANK_URL = "bankUrl"
private const val PREF_KEY_USERNAME = "username"
private const val PREF_KEY_PASSWORD = "password"
private const val PREF_KEY_CURRENCY = "currency"

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    val configDestination = ConfigFragmentDirections.actionGlobalConfigFragment()

    private val masterKeyAlias = MasterKeys.getOrCreate(AES256_GCM_SPEC)
    private val prefs = EncryptedSharedPreferences.create(
        PREF_NAME, masterKeyAlias, app, AES256_SIV, AES256_GCM
    )

    internal var config = Config(
        bankUrl = prefs.getString(PREF_KEY_BANK_URL, "")!!,
        username = prefs.getString(PREF_KEY_USERNAME, "")!!,
        password = prefs.getString(PREF_KEY_PASSWORD, "")!!
    )

    private val mCurrency = MutableLiveData<String>(
        prefs.getString(PREF_KEY_CURRENCY, null)
    )
    internal val currency: LiveData<String> = mCurrency

    private val mConfigResult = MutableLiveData<ConfigResult>()
    val configResult: LiveData<ConfigResult> = mConfigResult

    private val mBalance = MutableLiveData<BalanceResult>()
    val balance: LiveData<BalanceResult> = mBalance

    internal val withdrawManager = WithdrawManager(app, this)

    fun hasConfig() = config.bankUrl.isNotEmpty()
            && config.username.isNotEmpty()
            && config.password.isNotEmpty()

    /**
     * Start observing [configResult] after calling this to get the result async.
     * Warning: Ignore null results that are used to reset old results.
     */
    @UiThread
    fun checkAndSaveConfig(config: Config) {
        mConfigResult.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val url = "${config.bankUrl}/config"
            Log.d(TAG, "Checking config: $url")
            val result = when (val response = makeJsonGetRequest(url, config)) {
                is HttpJsonResult.Success -> {
                    // check if bank's version is compatible with app
                    val version = response.json.getString("version")
                    val versionIncompatible = VERSION_BANK.getIncompatibleStringOrNull(app, version)
                    if (versionIncompatible != null) {
                        ConfigResult.Error(false, versionIncompatible)
                    } else {
                        val currency = response.json.getString("currency")
                        try {
                            mCurrency.postValue(currency)
                            prefs.edit().putString(PREF_KEY_CURRENCY, currency).apply()
                            // save config
                            saveConfig(config)
                            ConfigResult.Success
                        } catch (e: Exception) {
                            ConfigResult.Error(false, "Invalid Config: ${response.json}")
                        }
                    }
                }
                is HttpJsonResult.Error -> {
                    if (response.statusCode > 0 && app.isOnline()) {
                        ConfigResult.Error(response.statusCode == 401, response.msg)
                    } else {
                        ConfigResult.Offline
                    }
                }
            }
            mConfigResult.postValue(result)
        }
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    private fun saveConfig(config: Config) {
        this.config = config
        prefs.edit()
            .putString(PREF_KEY_BANK_URL, config.bankUrl)
            .putString(PREF_KEY_USERNAME, config.username)
            .putString(PREF_KEY_PASSWORD, config.password)
            .commit()
    }

    fun getBalance() = viewModelScope.launch(Dispatchers.IO) {
        check(hasConfig()) { "No config to get balance" }
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
        saveConfig(config.copy(password = ""))
    }

}

data class Config(
    val bankUrl: String,
    val username: String,
    val password: String
)

sealed class ConfigResult {
    class Error(val authError: Boolean, val msg: String) : ConfigResult()
    object Offline : ConfigResult()
    object Success : ConfigResult()
}
