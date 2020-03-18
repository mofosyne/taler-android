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

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request.Method.GET
import com.android.volley.RequestQueue
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.merchantpos.Amount
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.config.MerchantRequest
import org.json.JSONObject

@JsonInclude(NON_EMPTY)
class Timestamp(
    @JsonProperty("t_ms")
    val ms: Long
)

data class HistoryItem(
    @JsonProperty("order_id")
    val orderId: String,
    @JsonProperty("amount")
    val amountStr: String,
    val summary: String,
    val timestamp: Timestamp
) {
    @get:JsonIgnore
    val amount: Amount by lazy { Amount.fromString(amountStr) }

    @get:JsonIgnore
    val time = timestamp.ms
}

sealed class HistoryResult {
    object Error : HistoryResult()
    class Success(val items: List<HistoryItem>) : HistoryResult()
}

class HistoryManager(
    private val configManager: ConfigManager,
    private val queue: RequestQueue,
    private val mapper: ObjectMapper
) {

    private val mIsLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = mIsLoading

    private val mItems = MutableLiveData<HistoryResult>()
    val items: LiveData<HistoryResult> = mItems

    @UiThread
    internal fun fetchHistory() {
        mIsLoading.value = true
        val merchantConfig = configManager.merchantConfig!!
        val params = mapOf("instance" to merchantConfig.instance)
        val req = MerchantRequest(GET, merchantConfig, "history", params, null,
            Listener { onHistoryResponse(it) },
            ErrorListener { onHistoryError() })
        queue.add(req)
    }

    @UiThread
    private fun onHistoryResponse(body: JSONObject) {
        mIsLoading.value = false
        val items = arrayListOf<HistoryItem>()
        val historyJson = body.getJSONArray("history")
        for (i in 0 until historyJson.length()) {
            val historyItem: HistoryItem = mapper.readValue(historyJson.getString(i))
            items.add(historyItem)
        }
        mItems.value = HistoryResult.Success(items)
    }

    @UiThread
    private fun onHistoryError() {
        mIsLoading.value = false
        mItems.value = HistoryResult.Error
    }

}
