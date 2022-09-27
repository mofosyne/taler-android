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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonArray
import net.taler.wallet.TAG
import net.taler.wallet.backend.ApiResponse
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

open class PendingOperationInfo(
    val type: String,
    val detail: JSONObject,
)

class PendingOperationsManager(
    private val walletBackendApi: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    val pendingOperations = MutableLiveData<List<PendingOperationInfo>>()

    internal fun getPending() {
        scope.launch {
            val response = walletBackendApi.sendRequest("getPendingOperations")
            if (response is ApiResponse.Error) {
                Log.i(TAG, "got getPending error result: ${response.error}")
                return@launch
            } else if (response is ApiResponse.Response) {
                Log.i(TAG, "got getPending result")
                val pendingList = mutableListOf<PendingOperationInfo>()
                val pendingJson = response.result["pendingOperations"]?.jsonArray ?: return@launch
                for (i in 0 until pendingJson.size) {
                    val p = JSONObject(pendingJson[i].toString())
                    val type = p.getString("type")
                    pendingList.add(PendingOperationInfo(type, p))
                }
                Log.i(TAG, "Got ${pendingList.size} pending operations")
                pendingOperations.postValue((pendingList))
            }
        }
    }

    fun retryPendingNow() {
        scope.launch {
            walletBackendApi.sendRequest("retryPendingNow")
        }
    }

}
