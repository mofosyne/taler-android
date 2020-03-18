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
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.config.MerchantRequest
import org.json.JSONObject

sealed class RefundResult {
    object Error : RefundResult()
    object PastDeadline : RefundResult()
    class Success(
        val refundUri: String,
        val item: HistoryItem,
        val amount: Double,
        val reason: String
    ) : RefundResult()
}

class RefundManager(
    private val configManager: ConfigManager,
    private val queue: RequestQueue
) {

    var toBeRefunded: HistoryItem? = null
        private set

    private val mRefundResult = MutableLiveData<RefundResult>()
    internal val refundResult: LiveData<RefundResult> = mRefundResult

    @UiThread
    internal fun startRefund(item: HistoryItem) {
        toBeRefunded = item
        mRefundResult.value = null
    }

    @UiThread
    internal fun refund(item: HistoryItem, amount: Double, reason: String) {
        val merchantConfig = configManager.merchantConfig!!
        val refundRequest = mapOf(
            "order_id" to item.orderId,
            "refund" to "${item.amount.currency}:$amount",
            "reason" to reason
        )
        val body = JSONObject(refundRequest)
        val req = MerchantRequest(POST, merchantConfig, "refund", null, body,
            Listener { onRefundResponse(it, item, amount, reason) },
            ErrorListener { onRefundError() }
        )
        queue.add(req)
    }

    @UiThread
    private fun onRefundResponse(
        json: JSONObject,
        item: HistoryItem,
        amount: Double,
        reason: String
    ) {
        if (!json.has("contract_terms")) {
            Log.e("TEST", "json: $json")
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
    private fun onRefundError() {
        mRefundResult.value = RefundResult.Error
    }

}
