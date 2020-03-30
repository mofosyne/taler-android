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

package net.taler.wallet.refund

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

sealed class RefundStatus {
    object Error : RefundStatus()
    object Success : RefundStatus()
}

class RefundManager(private val walletBackendApi: WalletBackendApi) {

    fun refund(refundUri: String): LiveData<RefundStatus> {
        val liveData = MutableLiveData<RefundStatus>()
        val args = JSONObject().also { it.put("talerRefundUri", refundUri) }
        walletBackendApi.sendRequest("applyRefund", args) { isError, result ->
            if (isError) {
                Log.e(TAG, "Refund Error: $result")
                liveData.postValue(RefundStatus.Error)
            } else {
                Log.e(TAG, "Refund Success: $result")
                liveData.postValue(RefundStatus.Success)
            }
        }
        return liveData
    }

}
