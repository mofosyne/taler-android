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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import net.taler.wallet.backend.TalerErrorCode.NONE
import net.taler.wallet.backend.WalletBackendService.Companion.MSG_COMMAND
import net.taler.wallet.backend.WalletBackendService.Companion.MSG_NOTIFY
import net.taler.wallet.backend.WalletBackendService.Companion.MSG_REPLY
import net.taler.wallet.backend.WalletBackendService.Companion.MSG_SUBSCRIBE_NOTIFY
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WalletBackendApi(
    private val app: Application,
    private val notificationHandler: ((payload: JSONObject) -> Unit),
) {
    private var walletBackendMessenger: Messenger? = null
    private val queuedMessages = LinkedList<Message>()
    private val handlers = ConcurrentHashMap<Int, (isError: Boolean, message: JSONObject) -> Unit>()
    private var nextRequestID = AtomicInteger(0)
    private val incomingMessenger = Messenger(IncomingHandler(this))

    private val walletBackendConn = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.w(TAG, "wallet backend service disconnected (crash?)")
            walletBackendMessenger = null
        }

        override fun onServiceConnected(componentName: ComponentName?, binder: IBinder?) {
            Log.i(TAG, "connected to wallet backend service")
            val bm = Messenger(binder)
            walletBackendMessenger = bm
            pumpQueue(bm)
            val msg = Message.obtain(null, MSG_SUBSCRIBE_NOTIFY)
            msg.replyTo = incomingMessenger
            bm.send(msg)
        }
    }

    init {
        Intent(app, WalletBackendService::class.java).also { intent ->
            app.bindService(intent, walletBackendConn, Context.BIND_AUTO_CREATE)
        }
    }

    private class IncomingHandler(strongApi: WalletBackendApi) : Handler() {
        private val weakApi = WeakReference(strongApi)
        override fun handleMessage(msg: Message) {
            val api = weakApi.get() ?: return
            when (msg.what) {
                MSG_REPLY -> {
                    val requestID = msg.data.getInt("requestID", 0)
                    val operation = msg.data.getString("operation", "??")
                    Log.i(TAG, "got reply for operation $operation ($requestID)")
                    val h = api.handlers.remove(requestID)
                    if (h == null) {
                        Log.e(TAG, "request ID not associated with a handler")
                        return
                    }
                    val response = msg.data.getString("response")
                    if (response == null) {
                        Log.e(TAG, "response did not contain response payload")
                        return
                    }
                    val isError = msg.data.getBoolean("isError")
                    val json = JSONObject(response)
                    h(isError, json)
                }
                MSG_NOTIFY -> {
                    val payloadStr = msg.data.getString("payload")
                    if (payloadStr == null) {
                        Log.e(TAG, "Notification had no payload: $msg")
                    } else {
                        val payload = JSONObject(payloadStr)
                        api.notificationHandler.invoke(payload)
                    }
                }
            }
        }
    }

    private fun pumpQueue(bm: Messenger) {
        while (true) {
            val msg = queuedMessages.pollFirst() ?: return
            bm.send(msg)
        }
    }

    fun sendRequest(
        operation: String,
        args: JSONObject? = null,
        onResponse: (isError: Boolean, message: JSONObject) -> Unit = { _, _ -> },
    ) {
        val requestID = nextRequestID.incrementAndGet()
        Log.i(TAG, "sending request for operation $operation ($requestID)\n${args?.toString(2)}")
        val msg = Message.obtain(null, MSG_COMMAND)
        handlers[requestID] = onResponse
        msg.replyTo = incomingMessenger
        val data = msg.data
        data.putString("operation", operation)
        data.putInt("requestID", requestID)
        if (args != null) {
            data.putString("args", args.toString())
        }
        val bm = walletBackendMessenger
        if (bm != null) {
            bm.send(msg)
        } else {
            queuedMessages.add(msg)
        }
    }

    suspend inline fun <reified T> request(
        operation: String,
        serializer: KSerializer<T>? = null,
        noinline args: (JSONObject.() -> JSONObject)? = null,
    ): WalletResponse<T> = withContext(Dispatchers.Default) {
        suspendCoroutine { cont ->
            val json = Json {
                ignoreUnknownKeys = true
            }
            sendRequest(operation, args?.invoke(JSONObject())) { isError, message ->
                val response = try {
                    if (isError) {
                        val error =
                            json.decodeFromString(TalerErrorInfo.serializer(), message.toString())
                        WalletResponse.Error(error)
                    } else {
                        val t: T = serializer?.let {
                            json.decodeFromString(serializer, message.toString())
                        } ?: Unit as T
                        WalletResponse.Success(t)
                    }
                } catch (e: Exception) {
                    val info = TalerErrorInfo(NONE, "", e.toString())
                    WalletResponse.Error(info)
                }
                cont.resume(response)
            }
        }
    }

    fun destroy() {
        // FIXME: implement this!
    }

    companion object {
        const val TAG = "WalletBackendApi"
    }
}
