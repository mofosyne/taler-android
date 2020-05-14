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

package net.taler.wallet.history

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject
import java.util.*

sealed class HistoryResult {
    object Error : HistoryResult()
    class Success(val history: List<HistoryEvent>) : HistoryResult()
}

class DevHistoryManager(
    private val walletBackendApi: WalletBackendApi,
    private val scope: CoroutineScope,
    private val mapper: ObjectMapper
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    private val mHistory = MutableLiveData<HistoryResult>()
    val history: LiveData<HistoryResult> = mHistory

    @UiThread
    internal fun loadHistory() {
        mProgress.value = true
        walletBackendApi.sendRequest("getHistory", null) { isError, result ->
            scope.launch(Dispatchers.Default) {
                onEventsLoaded(isError, result)
            }
        }
    }

    private fun onEventsLoaded(isError: Boolean, result: JSONObject) {
        if (isError) {
            mHistory.postValue(HistoryResult.Error)
            return
        }
        val history = LinkedList<HistoryEvent>()
        val json = result.getJSONArray("history")
        for (i in 0 until json.length()) {
            val event: HistoryEvent = mapper.readValue(json.getString(i))
            event.json = json.getJSONObject(i)
            history.add(event)
        }
        history.reverse()  // show latest first
        mProgress.postValue(false)
        mHistory.postValue(HistoryResult.Success(history))
    }

}
