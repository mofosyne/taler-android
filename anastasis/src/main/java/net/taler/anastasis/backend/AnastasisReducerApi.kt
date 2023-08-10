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

package net.taler.anastasis.backend

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import net.taler.anastasis.models.DiscoveryCursor
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.shared.Utils.encodeToNativeJson
import net.taler.common.ApiResponse
import net.taler.common.ApiResponse.*
import net.taler.common.TalerErrorCode
import org.json.JSONObject

@OptIn(DelicateCoroutinesApi::class)
class AnastasisReducerApi() {

    private val backendManager = BackendManager()

    init {
        GlobalScope.launch(Dispatchers.IO) {
            backendManager.run()
        }
    }

    suspend fun sendRequest(operation: String, args: JSONObject? = null): ApiResponse {
        return backendManager.send(operation, args)
    }

    suspend inline fun startBackup(): ReducerState = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        when (val response = sendRequest("anastasisStartBackup")) {
            is Response -> json.decodeFromJsonElement(response.result)
            is Error -> error("invalid reducer response")
        }
    }

    suspend inline fun startRecovery(): ReducerState = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        when (val response = sendRequest("anastasisStartRecovery")) {
            is Response -> json.decodeFromJsonElement(response.result)
            is Error -> error("invalid reducer response")
        }
    }

    suspend inline fun discoverPolicies(
        state: ReducerState,
    ): WalletResponse<ReducerState> = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        val cursor = (state as? ReducerState.Recovery)?.discoveryState?.cursor
        val args = JSONObject().apply {
            put("state", JSONObject(json.encodeToString(ReducerState.serializer(), state)))
            if (cursor != null)
                put("cursor", JSONObject(json.encodeToString(DiscoveryCursor.serializer(), cursor)))
        }
        handleResponse(sendRequest("anastasisDiscoverPolicies", args))
    }

    suspend inline fun reduceAction(
        state: ReducerState,
        action: String,
        noinline args: (JSONObject.() -> JSONObject)? = null,
    ): WalletResponse<ReducerState> = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        val body = JSONObject().apply {
            put("state", JSONObject(json.encodeToString(ReducerState.serializer(), state)))
            put("action", action)
            if (args != null) put("args", args.invoke(JSONObject()))
        }
        handleResponse(sendRequest("anastasisReduce", body))
    }

    suspend inline fun <reified T> reduceAction(
        state: ReducerState,
        action: String,
        args: T? = null,
    ): WalletResponse<ReducerState> = withContext(Dispatchers.Default) {
        val json = BackendManager.json
        val body = JSONObject().apply {
            put("state", json.encodeToNativeJson(state))
            put("action", action)
            if (args != null) put("args", json.encodeToNativeJson(args))
        }
        handleResponse(sendRequest("anastasisReduce", body))
    }

    fun handleResponse(response: ApiResponse): WalletResponse<ReducerState>  {
        val json = BackendManager.json
        return when (response) {
            is Response -> {
                when (val t = json.decodeFromJsonElement<ReducerState>(response.result)) {
                    is ReducerState.Recovery, is ReducerState.Backup -> {
                        WalletResponse.Success(t)
                    }
                    is ReducerState.Error -> {
                        WalletResponse.Error(
                            TalerErrorInfo(
                                code = TalerErrorCode.fromInt(t.code),
                                hint = t.hint,
                            )
                        )
                    }
                }
            }
            is Error -> {
                val error: TalerErrorInfo = json.decodeFromJsonElement(response.error)
                WalletResponse.Error(error)
            }
        }
    }
}
