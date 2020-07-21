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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request.Method.GET
import com.android.volley.RequestQueue
import com.android.volley.Response.Listener
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.common.Version
import net.taler.common.getIncompatibleStringOrNull
import net.taler.merchantlib.ConfigResponse
import net.taler.merchantlib.MerchantApi
import net.taler.merchantpos.LogErrorListener
import net.taler.merchantpos.R
import org.json.JSONObject

private const val SETTINGS_NAME = "taler-merchant-terminal"

private const val SETTINGS_CONFIG_URL = "configUrl"
private const val SETTINGS_USERNAME = "username"
private const val SETTINGS_PASSWORD = "password"

internal const val CONFIG_URL_DEMO = "https://docs.taler.net/_static/sample-pos-config.json"
internal const val CONFIG_USERNAME_DEMO = ""
internal const val CONFIG_PASSWORD_DEMO = ""

private val VERSION = Version(1, 0, 0)

private val TAG = ConfigManager::class.java.simpleName

interface ConfigurationReceiver {
    /**
     * Returns null if the configuration was valid, or a error string for user display otherwise.
     */
    suspend fun onConfigurationReceived(json: JSONObject, currency: String): String?
}

class ConfigManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val api: MerchantApi,
    private val mapper: ObjectMapper,
    private val queue: RequestQueue
) {

    private val prefs = context.getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE)
    private val configurationReceivers = ArrayList<ConfigurationReceiver>()

    var config = Config(
        configUrl = prefs.getString(SETTINGS_CONFIG_URL, CONFIG_URL_DEMO)!!,
        username = prefs.getString(SETTINGS_USERNAME, CONFIG_USERNAME_DEMO)!!,
        password = prefs.getString(SETTINGS_PASSWORD, CONFIG_PASSWORD_DEMO)!!
    )
    var merchantConfig: MerchantConfig? = null
        private set

    private val mConfigUpdateResult = MutableLiveData<ConfigUpdateResult>()
    val configUpdateResult: LiveData<ConfigUpdateResult> = mConfigUpdateResult

    fun addConfigurationReceiver(receiver: ConfigurationReceiver) {
        configurationReceivers.add(receiver)
    }

    @UiThread
    fun fetchConfig(config: Config, save: Boolean, savePassword: Boolean = false) {
        mConfigUpdateResult.value = null
        val configToSave = if (save) {
            if (savePassword) config else config.copy(password = "")
        } else null

        val stringRequest = object : JsonObjectRequest(GET, config.configUrl, null,
            Listener { onConfigReceived(it, configToSave) },
            LogErrorListener { onNetworkError(it) }
        ) {
            // send basic auth header
            override fun getHeaders(): MutableMap<String, String> {
                val credentials = "${config.username}:${config.password}"
                val auth = ("Basic ${encodeToString(credentials.toByteArray(), NO_WRAP)}")
                return mutableMapOf("Authorization" to auth)
            }
        }
        queue.add(stringRequest)
    }

    @UiThread
    private fun onConfigReceived(json: JSONObject, config: Config?) {
        val merchantConfig: MerchantConfig = try {
            mapper.readValue(json.getString("config"))
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing merchant config", e)
            val msg = context.getString(R.string.config_error_malformed)
            mConfigUpdateResult.value = ConfigUpdateResult.Error(msg)
            return
        }

        scope.launch(Dispatchers.IO) {
            val configResponse = api.getConfig(merchantConfig.baseUrl)
            onMerchantConfigReceived(config, json, merchantConfig, configResponse)
        }
    }

    private fun onMerchantConfigReceived(
        newConfig: Config?,
        configJson: JSONObject,
        merchantConfig: MerchantConfig,
        configResponse: ConfigResponse
    ) = scope.launch(Dispatchers.Default) {
        val versionIncompatible = VERSION.getIncompatibleStringOrNull(context, configResponse.version)
        if (versionIncompatible != null) {
            mConfigUpdateResult.postValue(ConfigUpdateResult.Error(versionIncompatible))
            return@launch
        }
        for (receiver in configurationReceivers) {
            val result = try {
                receiver.onConfigurationReceived(configJson, configResponse.currency)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling configuration by ${receiver::class.java.simpleName}", e)
                context.getString(R.string.config_error_unknown)
            }
            if (result != null) {  // error
                mConfigUpdateResult.postValue(ConfigUpdateResult.Error(result))
                return@launch
            }
        }
        newConfig?.let {
            config = it
            saveConfig(it)
        }
        this@ConfigManager.merchantConfig = merchantConfig.copy(currency = configResponse.currency)
        mConfigUpdateResult.postValue(ConfigUpdateResult.Success(configResponse.currency))
    }

    fun forgetPassword() {
        config = config.copy(password = "")
        saveConfig(config)
        merchantConfig = null
    }

    private fun saveConfig(config: Config) {
        prefs.edit()
            .putString(SETTINGS_CONFIG_URL, config.configUrl)
            .putString(SETTINGS_USERNAME, config.username)
            .putString(SETTINGS_PASSWORD, config.password)
            .apply()
    }

    @UiThread
    private fun onNetworkError(it: VolleyError?) {
        val msg = context.getString(
            if (it?.networkResponse?.statusCode == 401) R.string.config_auth_error
            else R.string.config_error_network
        )
        mConfigUpdateResult.value = ConfigUpdateResult.Error(msg)
    }

}

sealed class ConfigUpdateResult {
    data class Error(val msg: String) : ConfigUpdateResult()
    data class Success(val currency: String) : ConfigUpdateResult()
}
