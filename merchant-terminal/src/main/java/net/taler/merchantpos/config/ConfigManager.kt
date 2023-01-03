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

package net.taler.merchantpos.config

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Base64.NO_WRAP
import android.util.Base64.encodeToString
import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.common.Version
import net.taler.common.getIncompatibleStringOrNull
import net.taler.merchantlib.ConfigResponse
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.MerchantConfig
import net.taler.merchantpos.R

private const val SETTINGS_NAME = "taler-merchant-terminal"

private const val SETTINGS_CONFIG_URL = "configUrl"
private const val SETTINGS_USERNAME = "username"
private const val SETTINGS_PASSWORD = "password"

internal const val CONFIG_URL_DEMO = "https://docs.taler.net/_static/sample-pos-config.json"
internal const val CONFIG_USERNAME_DEMO = ""
internal const val CONFIG_PASSWORD_DEMO = ""

private val VERSION = Version(3, 0, 1)

private val TAG = ConfigManager::class.java.simpleName

interface ConfigurationReceiver {
    /**
     * Returns null if the configuration was valid, or a error string for user display otherwise.
     */
    suspend fun onConfigurationReceived(posConfig: PosConfig, currency: String): String?
}

class ConfigManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val httpClient: HttpClient,
    private val api: MerchantApi
) {

    private val prefs = context.getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
    private val configurationReceivers = ArrayList<ConfigurationReceiver>()

    var config = Config(
        configUrl = prefs.getString(SETTINGS_CONFIG_URL, CONFIG_URL_DEMO)!!,
        username = prefs.getString(SETTINGS_USERNAME, CONFIG_USERNAME_DEMO)!!,
        password = prefs.getString(SETTINGS_PASSWORD, CONFIG_PASSWORD_DEMO)!!
    )
    @Volatile
    var merchantConfig: MerchantConfig? = null
        private set
    @Volatile
    var currency: String? = null
        private set

    private val mConfigUpdateResult = MutableLiveData<ConfigUpdateResult?>()
    val configUpdateResult: LiveData<ConfigUpdateResult?> = mConfigUpdateResult

    fun addConfigurationReceiver(receiver: ConfigurationReceiver) {
        configurationReceivers.add(receiver)
    }

    @UiThread
    fun fetchConfig(config: Config, save: Boolean, savePassword: Boolean = false) {
        mConfigUpdateResult.value = null
        val configToSave = if (save) {
            if (savePassword) config else config.copy(password = "")
        } else null

        scope.launch(Dispatchers.IO) {
            try {
                // get PoS configuration
                val posConfig: PosConfig = httpClient.get(config.configUrl) {
                    val credentials = "${config.username}:${config.password}"
                    val auth = ("Basic ${encodeToString(credentials.toByteArray(), NO_WRAP)}")
                    header(Authorization, auth)
                }.body()
                val merchantConfig = posConfig.merchantConfig
                // get config from merchant backend API
                api.getConfig(merchantConfig.baseUrl).handleSuspend(::onNetworkError) {
                    onMerchantConfigReceived(configToSave, posConfig, merchantConfig, it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error retrieving merchant config", e)
                val msg = if (e is ClientRequestException) {
                    context.getString(
                        if (e.response.status == Unauthorized) R.string.config_auth_error
                        else R.string.config_error_network
                    )
                } else {
                    context.getString(R.string.config_error_malformed)
                }
                onNetworkError(msg)
            }
        }
    }

    @WorkerThread
    private suspend fun onMerchantConfigReceived(
        newConfig: Config?,
        posConfig: PosConfig,
        merchantConfig: MerchantConfig,
        configResponse: ConfigResponse
    ) {
        val versionIncompatible =
            VERSION.getIncompatibleStringOrNull(context, configResponse.version)
        if (versionIncompatible != null) {
            Log.e(TAG, "Versions incompatible $configResponse")
            mConfigUpdateResult.postValue(ConfigUpdateResult.Error(versionIncompatible))
            return
        }
        for (receiver in configurationReceivers) {
            val result = try {
                receiver.onConfigurationReceived(posConfig, configResponse.currency)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling configuration by ${receiver::class.java.simpleName}", e)
                context.getString(R.string.config_error_unknown)
            }
            if (result != null) { // error
                mConfigUpdateResult.postValue(ConfigUpdateResult.Error(result))
                return
            }
        }
        newConfig?.let {
            config = it
            saveConfig(it)
        }
        this.merchantConfig = merchantConfig
        this.currency = configResponse.currency
        mConfigUpdateResult.postValue(ConfigUpdateResult.Success(configResponse.currency))
    }

    @UiThread
    fun forgetPassword() {
        config = config.copy(password = "")
        saveConfig(config)
        merchantConfig = null
    }

    @UiThread
    private fun saveConfig(config: Config) {
        prefs.edit()
            .putString(SETTINGS_CONFIG_URL, config.configUrl)
            .putString(SETTINGS_USERNAME, config.username)
            .putString(SETTINGS_PASSWORD, config.password)
            .apply()
    }

    private fun onNetworkError(msg: String) = scope.launch(Dispatchers.Main) {
        mConfigUpdateResult.value = ConfigUpdateResult.Error(msg)
    }
}

sealed class ConfigUpdateResult {
    data class Error(val msg: String) : ConfigUpdateResult()
    data class Success(val currency: String) : ConfigUpdateResult()
}
