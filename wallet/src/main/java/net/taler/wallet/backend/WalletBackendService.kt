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

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import net.taler.qtart.TalerWalletCore
import net.taler.wallet.BuildConfig
import net.taler.wallet.HostCardEmulatorService
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.exitProcess

private const val TAG = "taler-wallet-backend"
const val WALLET_DB = "talerwalletdb-v30.json"

class RequestData(val clientRequestId: Int, val messenger: Messenger)


class WalletBackendService : Service() {
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    private val messenger: Messenger = Messenger(IncomingHandler(this))

    private val walletCore = TalerWalletCore()

    private var initialized = false

    private var nextRequestID = 1

    private val requests = ConcurrentHashMap<Int, RequestData>()

    private val subscribers = LinkedList<Messenger>()

    override fun onCreate() {
        Log.i(TAG, "onCreate in wallet backend service")

        walletCore.setMessageHandler {
            this@WalletBackendService.handleAkonoMessage(it)
        }
        if (BuildConfig.DEBUG) walletCore.setStdoutHandler {
            Log.d(TAG, it)
        }
        walletCore.run()
        sendInitMessage()
        // runIntegrationTest()
        super.onCreate()
    }

    private fun sendInitMessage() {
        val msg = JSONObject()
        msg.put("operation", "init")
        val args = JSONObject()
        msg.put("args", args)
        args.put("persistentStoragePath", "${application.filesDir}/$WALLET_DB")
        args.put("logLevel", "INFO")
        Log.d(TAG, "init message: ${msg.toString(2)}")
        walletCore.sendRequest(msg.toString())
    }

    /**
     * Run the integration tests for wallet-core.
     */
    private fun runIntegrationTest() {
        val msg = JSONObject()
        msg.put("operation", "runIntegrationTest")
        val args = JSONObject()
        msg.put("args", args)
        args.put("amountToWithdraw", "KUDOS:3")
        args.put("amountToSpend", "KUDOS:1")
        args.put("bankBaseUrl", "https://bank.demo.taler.net/demobanks/default/access-api/")
        args.put("exchangeBaseUrl", "https://exchange.demo.taler.net/")
        args.put("merchantBaseUrl", "https://backend.demo.taler.net/")
        args.put("merchantAuthToken", "secret-token:sandbox")
        Log.d(TAG, "integration test message: ${msg.toString(2)}")
        walletCore.sendRequest(msg.toString())
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler(
        service: WalletBackendService,
    ) : Handler() {

        private val serviceWeakRef = WeakReference(service)

        override fun handleMessage(msg: Message) {
            val svc = serviceWeakRef.get() ?: return
            if (!svc.initialized) Log.w(TAG, "Warning: Not yet initialized")
            when (msg.what) {
                MSG_COMMAND -> {
                    val data = msg.data
                    val serviceRequestID = svc.nextRequestID++
                    val clientRequestID = data.getInt("requestID", 0)
                    if (clientRequestID == 0) {
                        Log.e(TAG, "client requestID missing")
                        return
                    }
                    val args = data.getString("args")
                    val argsObj = if (args == null) {
                        JSONObject()
                    } else {
                        JSONObject(args)
                    }
                    val operation = data.getString("operation", "")
                    if (operation == "") {
                        Log.e(TAG, "client command missing")
                        return
                    }
                    Log.i(TAG, "got request for operation $operation")
                    val request = JSONObject()
                    request.put("operation", operation)
                    request.put("id", serviceRequestID)
                    request.put("args", argsObj)
                    svc.walletCore.sendRequest(request.toString())
                    Log.i(
                        TAG,
                        "mapping service request ID $serviceRequestID to client request ID $clientRequestID"
                    )
                    svc.requests[serviceRequestID] = RequestData(clientRequestID, msg.replyTo)
                }
                MSG_SUBSCRIBE_NOTIFY -> {
                    Log.i(TAG, "subscribing client")
                    val r = msg.replyTo
                    if (r == null) {
                        Log.e(
                            TAG,
                            "subscriber did not specify replyTo object in MSG_SUBSCRIBE_NOTIFY"
                        )
                    } else {
                        svc.subscribers.add(msg.replyTo)
                    }
                }
                MSG_UNSUBSCRIBE_NOTIFY -> {
                    Log.i(TAG, "unsubscribing client")
                    svc.subscribers.remove(msg.replyTo)
                }
                else -> {
                    Log.e(TAG, "unknown message from client")
                    super.handleMessage(msg)
                }
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return messenger.binder
    }

    private fun sendNotify(payload: String) {
        var rm: LinkedList<Messenger>? = null
        for (s in subscribers) {
            val m = Message.obtain(null, MSG_NOTIFY)
            val b = m.data
            b.putString("payload", payload)
            try {
                s.send(m)
            } catch (e: RemoteException) {
                if (rm == null) {
                    rm = LinkedList()
                }
                rm.add(s)
                subscribers.remove(s)
            }
        }
        if (rm != null) {
            for (s in rm) {
                subscribers.remove(s)
            }
        }
    }

    private fun handleAkonoMessage(messageStr: String) {
        val message = JSONObject(messageStr)
        when (val type = message.getString("type")) {
            "notification" -> {
                val payload = message.getJSONObject("payload")
                if (payload.optString("type") != "waiting-for-retry") {
                    Log.v(TAG, "got back notification: ${message.toString(2)}")
                }
                sendNotify(payload.toString())
            }
            "tunnelHttp" -> {
                Log.v(TAG, "got http tunnel request! ${message.toString(2)}")
                Intent().also { intent ->
                    intent.action = HostCardEmulatorService.HTTP_TUNNEL_REQUEST
                    intent.putExtra("tunnelMessage", messageStr)
                    application.sendBroadcast(intent)
                }
            }
            "response" -> {
                when (message.getString("operation")) {
                    "init" -> {
                        Log.d(TAG, "got response for init operation: ${message.toString(2)}")
                        initialized = true
                        sendNotify(message.toString(2))
                    }
                    "reset" -> {
                        Log.v(TAG, "got back message: ${message.toString(2)}")
                        exitProcess(1)
                    }
                    else -> {
                        Log.v(TAG, "got back response: ${message.toString(2)}")
                        val payload = message.getJSONObject("result").toString(2)
                        handleResponse(false, message, payload)
                    }
                }
            }
            "error" -> {
                Log.v(TAG, "got back error: ${message.toString(2)}")
                val payload = message.getJSONObject("error").toString(2)
                handleResponse(true, message, payload)
            }
            else -> throw IllegalArgumentException("Unknown message type: $type")
        }
    }

    private fun handleResponse(isError: Boolean, message: JSONObject, payload: String) {
        val id = message.getInt("id")
        val rId = requests[id]
        if (rId == null) {
            Log.e(TAG, "wallet returned unknown request ID ($id)")
            return
        }
        val m = Message.obtain(null, MSG_REPLY)
        val b = m.data
        b.putInt("requestID", rId.clientRequestId)
        b.putBoolean("isError", isError)
        b.putString("response", payload)
        b.putString("operation", message.getString("operation"))
        rId.messenger.send(m)
    }

    companion object {
        const val MSG_SUBSCRIBE_NOTIFY = 1
        const val MSG_UNSUBSCRIBE_NOTIFY = 2
        const val MSG_COMMAND = 3
        const val MSG_REPLY = 4
        const val MSG_NOTIFY = 5
    }
}
