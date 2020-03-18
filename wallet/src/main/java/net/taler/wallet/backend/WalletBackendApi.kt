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
import android.util.SparseArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*

class WalletBackendApi(
    private val app: Application,
    private val onConnected: (() -> Unit),
    private val notificationHandler: (() -> Unit)
) {

    private var walletBackendMessenger: Messenger? = null
    private val queuedMessages = LinkedList<Message>()
    private val handlers = SparseArray<(isError: Boolean, message: JSONObject) -> Unit>()
    private var nextRequestID = 1

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
            val msg = Message.obtain(null, WalletBackendService.MSG_SUBSCRIBE_NOTIFY)
            msg.replyTo = incomingMessenger
            bm.send(msg)
            onConnected.invoke()
        }
    }

    private class IncomingHandler(strongApi: WalletBackendApi) : Handler() {
        private val weakApi = WeakReference(strongApi)
        override fun handleMessage(msg: Message) {
            val api = weakApi.get() ?: return
            when (msg.what) {
                WalletBackendService.MSG_REPLY -> {
                    val requestID = msg.data.getInt("requestID", 0)
                    val operation = msg.data.getString("operation", "??")
                    Log.i(TAG, "got reply for operation $operation ($requestID)")
                    val h = api.handlers.get(requestID)
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
                WalletBackendService.MSG_NOTIFY -> {
                    api.notificationHandler.invoke()
                }
            }
        }
    }

    private val incomingMessenger = Messenger(IncomingHandler(this))

    init {
        Intent(app, WalletBackendService::class.java).also { intent ->
            app.bindService(intent, walletBackendConn, Context.BIND_AUTO_CREATE)
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
        args: JSONObject?,
        onResponse: (isError: Boolean, message: JSONObject) -> Unit = { _, _ -> }
    ) {
        val requestID = nextRequestID++
        Log.i(TAG, "sending request for operation $operation ($requestID)")
        val msg = Message.obtain(null, WalletBackendService.MSG_COMMAND)
        handlers.put(requestID, onResponse)
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

    fun destroy() {
        // FIXME: implement this!
    }

    companion object {
        const val TAG = "WalletBackendApi"
    }
}
