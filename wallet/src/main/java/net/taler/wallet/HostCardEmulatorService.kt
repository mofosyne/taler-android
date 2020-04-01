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

package net.taler.wallet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentLinkedDeque

fun makeApduSuccessResponse(payload: ByteArray): ByteArray {
    val stream = ByteArrayOutputStream()
    stream.write(payload)
    stream.write(0x90)
    stream.write(0x00)
    return stream.toByteArray()
}


fun makeApduFailureResponse(): ByteArray {
    val stream = ByteArrayOutputStream()
    stream.write(0x6F)
    stream.write(0x00)
    return stream.toByteArray()
}


fun readApduBodySize(stream: ByteArrayInputStream): Int {
    val b0 = stream.read()
    if (b0 == -1) {
        return 0
    }
    if (b0 != 0) {
        return b0
    }
    val b1 = stream.read()
    val b2 = stream.read()

    return (b1 shl 8) and b2
}


class HostCardEmulatorService : HostApduService() {

    val queuedRequests: ConcurrentLinkedDeque<String> = ConcurrentLinkedDeque()
    private lateinit var receiver: BroadcastReceiver

    override fun onCreate() {
        super.onCreate()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                queuedRequests.addLast(p1!!.getStringExtra("tunnelMessage"))
            }
        }
        IntentFilter(HTTP_TUNNEL_REQUEST).also { filter ->
            registerReceiver(receiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
        Intent().also { intent ->
            intent.action = MERCHANT_NFC_DISCONNECTED
            sendBroadcast(intent)
        }
    }

    override fun processCommandApdu(
        commandApdu: ByteArray?,
        extras: Bundle?
    ): ByteArray {

        Log.d(TAG, "Processing command APDU")

        if (commandApdu == null) {
            Log.d(TAG, "APDU is null")
            return makeApduFailureResponse()
        }

        val stream = ByteArrayInputStream(commandApdu)

        val command = stream.read()

        if (command != 0) {
            Log.d(TAG, "APDU has invalid command")
            return makeApduFailureResponse()
        }

        val instruction = stream.read()

        // Read instruction parameters, currently ignored.
        stream.read()
        stream.read()

        if (instruction == SELECT_INS) {
            // FIXME: validate body!
            return makeApduSuccessResponse(ByteArray(0))
        }

        if (instruction == GET_INS) {
            val req = queuedRequests.poll()
            return if (req != null) {
                Log.v(TAG, "sending tunnel request")
                makeApduSuccessResponse(req.toByteArray(Charsets.UTF_8))
            } else {
                makeApduSuccessResponse(ByteArray(0))
            }
        }

        if (instruction == PUT_INS) {
            val bodySize = readApduBodySize(stream)
            val talerInstr = stream.read()
            val bodyBytes = stream.readBytes()
            if (1 + bodyBytes.size != bodySize) {
                Log.w(TAG, "mismatched body size ($bodySize vs ${bodyBytes.size}")
            }

            when (talerInstr) {
                1 -> {
                    val url = String(bodyBytes, Charsets.UTF_8)
                    Log.v(TAG, "got URL: '$url'")

                    Intent(this, MainActivity::class.java).also { intent ->
                        intent.data = Uri.parse(url)
                        intent.action = Intent.ACTION_VIEW
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                }
                2 -> {
                    Log.v(TAG, "got http response: ${bodyBytes.toString(Charsets.UTF_8)}")

                    Intent().also { intent ->
                        intent.action = HTTP_TUNNEL_RESPONSE
                        intent.putExtra("response", bodyBytes.toString(Charsets.UTF_8))
                        sendBroadcast(intent)
                    }
                }
                else -> {
                    Log.v(TAG, "taler instruction $talerInstr unknown")
                }
            }

            return makeApduSuccessResponse(ByteArray(0))
        }

        return makeApduFailureResponse()
    }

    companion object {
        const val TAG = "taler-wallet-hce"
        const val SELECT_INS = 0xA4
        const val PUT_INS = 0xDA
        const val GET_INS = 0xCA

        const val TRIGGER_PAYMENT_ACTION = "net.taler.TRIGGER_PAYMENT_ACTION"

        const val MERCHANT_NFC_CONNECTED = "net.taler.MERCHANT_NFC_CONNECTED"
        const val MERCHANT_NFC_DISCONNECTED = "net.taler.MERCHANT_NFC_DISCONNECTED"

        const val HTTP_TUNNEL_RESPONSE = "net.taler.HTTP_TUNNEL_RESPONSE"
        const val HTTP_TUNNEL_REQUEST = "net.taler.HTTP_TUNNEL_REQUEST"
    }
}