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

package net.taler.cashier.config

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
import androidx.security.crypto.MasterKeys
import androidx.security.crypto.MasterKeys.AES256_GCM_SPEC
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.taler.cashier.BuildConfig
import net.taler.cashier.Response
import net.taler.cashier.Response.Companion.response
import net.taler.common.Version
import net.taler.common.getIncompatibleStringOrNull

val VERSION_BANK = Version.parse(BuildConfig.BACKEND_API_VERSION)!!
private const val PREF_NAME = "net.taler.cashier.prefs"
private const val PREF_KEY_BANK_URL = "bankUrl"
private const val PREF_KEY_USERNAME = "username"
private const val PREF_KEY_PASSWORD = "password"
private const val PREF_KEY_CURRENCY = "currency"

private val TAG = ConfigManager::class.java.simpleName

class ConfigManager(
    private val app: Application,
    private val scope: CoroutineScope,
    private val httpClient: HttpClient,
) {

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

    private val mConfigResult = MutableLiveData<ConfigResult?>()
    val configResult: LiveData<ConfigResult?> = mConfigResult

    fun hasConfig() = config.bankUrl.isNotEmpty()
            && config.username.isNotEmpty()
            && config.password.isNotEmpty()

    /**
     * Start observing [configResult] after calling this to get the result async.
     * Warning: Ignore null results that are used to reset old results.
     */
    @UiThread
    fun checkAndSaveConfig(config: Config) = scope.launch {
        mConfigResult.value = null
        checkConfig(config).onError { failure ->
            val result = if (failure.isOffline(app)) {
                ConfigResult.Offline
            } else {
                ConfigResult.Error(failure.statusCode == Unauthorized, failure.msg)
            }
            mConfigResult.postValue(result)
        }.onSuccess { response ->
            val versionIncompatible =
                VERSION_BANK.getIncompatibleStringOrNull(app, response.version)
            val result = if (versionIncompatible != null) {
                ConfigResult.Error(false, versionIncompatible)
            } else {
                mCurrency.postValue(response.currency)
                prefs.edit().putString(PREF_KEY_CURRENCY, response.currency).apply()
                // save config
                saveConfig(config)
                ConfigResult.Success
            }
            mConfigResult.postValue(result)
        }
    }

    private suspend fun checkConfig(config: Config) = withContext(Dispatchers.IO) {
        val url = "${config.bankUrl}/config"
        Log.d(TAG, "Checking config: $url")
        val configResponse = response {
            httpClient.get(url).body<ConfigResponse>()
        }
        if (configResponse.isFailure) {
            configResponse
        } else {
            // we need to check an endpoint that requires authentication as well
            // to see if the credentials are valid
            val balanceResponse = response {
                val authUrl = "${config.bankUrl}/accounts/${config.username}"
                Log.d(TAG, "Checking auth: $authUrl")
                httpClient.get(authUrl) {
                    header(Authorization, config.basicAuth)
                }
            }
            @Suppress("UNCHECKED_CAST")  // The type doesn't matter for failures
            if (balanceResponse.isFailure) balanceResponse as Response<ConfigResponse>
            else configResponse
        }
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    internal fun saveConfig(config: Config) {
        this.config = config
        prefs.edit()
            .putString(PREF_KEY_BANK_URL, config.bankUrl)
            .putString(PREF_KEY_USERNAME, config.username)
            .putString(PREF_KEY_PASSWORD, config.password)
            .commit()
    }

    fun lock() {
        saveConfig(config.copy(password = ""))
    }

}
