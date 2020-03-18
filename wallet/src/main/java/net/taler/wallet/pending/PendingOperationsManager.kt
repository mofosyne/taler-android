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

package net.taler.wallet.pending

import android.util.Log
import androidx.lifecycle.MutableLiveData
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

open class PendingOperationInfo(
    val type: String,
    val detail: JSONObject
)

class PendingOperationsManager(private val walletBackendApi: WalletBackendApi) {

    private var activeGetPending = 0

    val pendingOperations = MutableLiveData<List<PendingOperationInfo>>()

    internal fun getPending() {
        if (activeGetPending > 0) {
            return
        }
        activeGetPending++
        walletBackendApi.sendRequest("getPendingOperations", null) { isError, result ->
            activeGetPending--
            if (isError) {
                Log.i(TAG, "got getPending error result")
                return@sendRequest
            }
            Log.i(TAG, "got getPending result")
            val pendingList = mutableListOf<PendingOperationInfo>()
            val pendingJson = result.getJSONArray("pendingOperations")
            for (i in 0 until pendingJson.length()) {
                val p = pendingJson.getJSONObject(i)
                val type = p.getString("type")
                pendingList.add(PendingOperationInfo(type, p))
            }
            Log.i(TAG, "Got ${pendingList.size} pending operations")
            pendingOperations.postValue((pendingList))
        }
    }

    fun retryPendingNow() {
        walletBackendApi.sendRequest("retryPendingNow", null)
    }

}
