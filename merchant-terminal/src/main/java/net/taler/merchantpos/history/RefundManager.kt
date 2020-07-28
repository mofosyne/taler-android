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

package net.taler.merchantpos.history

import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request.Method.POST
import com.android.volley.RequestQueue
import com.android.volley.Response.Listener
import com.android.volley.VolleyError
import net.taler.common.Amount
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.LogErrorListener
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.config.MerchantRequest
import org.json.JSONObject

sealed class RefundResult {
    object Error : RefundResult()
    object PastDeadline : RefundResult()
    object AlreadyRefunded : RefundResult()
    class Success(
        val refundUri: String,
        val item: OrderHistoryEntry,
        val amount: Amount,
        val reason: String
    ) : RefundResult()
}

class RefundManager(
    private val configManager: ConfigManager,
    private val queue: RequestQueue
) {

    companion object {
        val TAG = RefundManager::class.java.simpleName
    }

    var toBeRefunded: OrderHistoryEntry? = null
        private set

    private val mRefundResult = MutableLiveData<RefundResult>()
    internal val refundResult: LiveData<RefundResult> = mRefundResult

    @UiThread
    internal fun startRefund(item: OrderHistoryEntry) {
        toBeRefunded = item
        mRefundResult.value = null
    }

    @UiThread
    internal fun abortRefund() {
        toBeRefunded = null
        mRefundResult.value = null
    }

    @UiThread
    internal fun refund(item: OrderHistoryEntry, amount: Amount, reason: String) {
        val merchantConfig = configManager.merchantConfig!!
        val refundRequest = mapOf(
            "order_id" to item.orderId,
            "refund" to amount.toJSONString(),
            "reason" to reason
        )
        val body = JSONObject(refundRequest)
        Log.d(TAG, body.toString(4))
        val req = MerchantRequest(POST, merchantConfig, "refund", null, body,
            Listener { onRefundResponse(it, item, amount, reason) },
            LogErrorListener { onRefundError(it) }
        )
        queue.add(req)
    }

    @UiThread
    private fun onRefundResponse(
        json: JSONObject,
        item: OrderHistoryEntry,
        amount: Amount,
        reason: String
    ) {
        if (!json.has("contract_terms")) {
            Log.e(TAG, "Contract terms missing: $json")
            onRefundError()
            return
        }

        val contractTerms = json.getJSONObject("contract_terms")
        val refundDeadline = if (contractTerms.has("refund_deadline")) {
            contractTerms.getJSONObject("refund_deadline").getLong("t_ms")
        } else null
        val autoRefund = contractTerms.has("auto_refund")
        val refundUri = json.getString("taler_refund_uri")

        Log.e("TEST", "refundDeadline: $refundDeadline")
        if (refundDeadline != null) Log.e(
            "TEST",
            "refundDeadline passed: ${System.currentTimeMillis() > refundDeadline}"
        )
        Log.e("TEST", "autoRefund: $autoRefund")
        Log.e("TEST", "refundUri: $refundUri")

        mRefundResult.value = RefundResult.Success(refundUri, item, amount, reason)
    }

    @UiThread
    private fun onRefundError(error: VolleyError? = null) {
        val data = error?.networkResponse?.data
        if (data != null) {
            val json = JSONObject(String(data))
            if (json.has("code") && json.getInt("code") == 2602) {
                mRefundResult.value = RefundResult.AlreadyRefunded
                return
            }
        }
        mRefundResult.value = RefundResult.Error
    }

}
