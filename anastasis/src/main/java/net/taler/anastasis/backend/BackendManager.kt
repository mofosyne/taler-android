/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

import android.util.Log
import kotlinx.serialization.json.Json
import net.taler.common.ApiMessage
import net.taler.common.ApiResponse
import net.taler.qtart.TalerWalletCore
import net.taler.anastasis.BuildConfig
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BackendManager {

    companion object {
        private const val TAG = "BackendManager"
        private const val TAG_CORE = "taler-wallet-embedded"
        val json = Json {
            ignoreUnknownKeys = true
        }
        @JvmStatic
        private val initialized = AtomicBoolean(false)
    }

    private val walletCore = TalerWalletCore()
    private val requestManager = RequestManager()

    init {
        if (initialized.getAndSet(true)) error("Already initialized")
        walletCore.setMessageHandler { onMessageReceived(it) }
        if (BuildConfig.DEBUG) walletCore.setStdoutHandler {
            Log.d(TAG_CORE, it)
        }
    }

    fun run() {
        walletCore.run()
    }

    suspend fun send(operation: String, args: JSONObject? = null): ApiResponse =
        suspendCoroutine { cont ->
            requestManager.addRequest(cont) { id ->
                val request = JSONObject().apply {
                    put("id", id)
                    put("operation", operation)
                    if (args != null) put("args", args)
                }
                Log.d(TAG, "sending message:\n${request.toString(2)}")
                walletCore.sendRequest(request.toString())
            }
        }

    private fun onMessageReceived(msg: String) {
        Log.d(TAG, "message received: $msg")
        when (val message = json.decodeFromString<ApiMessage>(msg)) {
            is ApiMessage.Notification -> {}
            is ApiResponse -> {
                val id = message.id
                val cont = requestManager.getAndRemoveContinuation(id)
                if (cont == null) {
                    Log.e(TAG, "wallet returned unknown request ID ($id)")
                } else {
                    cont.resume(message)
                }
            }
        }
    }
}
