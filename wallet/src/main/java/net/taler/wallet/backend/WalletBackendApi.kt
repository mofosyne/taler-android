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

package net.taler.wallet.backend

import android.app.Application
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import net.taler.wallet.backend.TalerErrorCode.NONE
import org.json.JSONObject
import java.io.File

private const val WALLET_DB = "talerwalletdb.sqlite3"

@OptIn(DelicateCoroutinesApi::class)
class WalletBackendApi(
    private val app: Application,
    private val versionReceiver: VersionReceiver,
    notificationReceiver: NotificationReceiver,
) {

    private val backendManager = BackendManager(notificationReceiver)

    init {
        GlobalScope.launch(Dispatchers.IO) {
            backendManager.run()
            sendInitMessage()
        }
    }

    private suspend fun sendInitMessage() {
        val db = if (File(app.filesDir, "talerwalletdb.sql").isFile) {
            // can be removed after a reasonable migration period (2024-02-02)
            "${app.filesDir}/talerwalletdb.sql"
        } else {
            "${app.filesDir}/${WALLET_DB}"
        }
        request("init", InitResponse.serializer()) {
            put("persistentStoragePath", db)
            put("logLevel", "INFO")
        }.onSuccess { response ->
            versionReceiver.onVersionReceived(response.versionInfo)
        }.onError { error ->
            // TODO expose this to the UI as it can happen when using an older DB version
            error("Error on init message: $error")
        }
    }

    suspend fun sendRequest(operation: String, args: JSONObject? = null): ApiResponse {
        return backendManager.send(operation, args)
    }

    suspend inline fun <reified T> request(
        operation: String,
        serializer: KSerializer<T>? = null,
        noinline args: (JSONObject.() -> JSONObject)? = null,
    ): WalletResponse<T> = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        try {
            when (val response = sendRequest(operation, args?.invoke(JSONObject()))) {
                is ApiResponse.Response -> {
                    val t: T = serializer?.let {
                        json.decodeFromJsonElement(serializer, response.result)
                    } ?: Unit as T
                    WalletResponse.Success(t)
                }

                is ApiResponse.Error -> {
                    val error: TalerErrorInfo = json.decodeFromJsonElement(response.error)
                    WalletResponse.Error(error)
                }
            }
        } catch (e: Exception) {
            val info = TalerErrorInfo(NONE, "", e.toString())
            WalletResponse.Error(info)
        }
    }

    // Returns raw JSON response instead of serialized object
    suspend inline fun rawRequest(
        operation: String,
        noinline args: (JSONObject.() -> JSONObject)? = null,
    ): WalletResponse<JsonObject> = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        try {
            when (val response = sendRequest(operation, args?.invoke(JSONObject()))) {
                is ApiResponse.Response -> {
                    WalletResponse.Success(response.result)
                }

                is ApiResponse.Error -> {
                    val error: TalerErrorInfo = json.decodeFromJsonElement(response.error)
                    WalletResponse.Error(error)
                }
            }
        } catch (e: Exception) {
            val info = TalerErrorInfo(NONE, "", e.toString())
            WalletResponse.Error(info)
        }
    }
}
