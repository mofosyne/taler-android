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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.lib.common.Amount
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi

sealed class RefundStatus {
    object Error : RefundStatus()
    data class Success(val response: RefundResponse) : RefundStatus()
}

@Serializable
data class RefundResponse(
    val amountEffectivePaid: Amount,
    val amountRefundGranted: Amount,
    val amountRefundGone: Amount,
    val pendingAtExchange: Boolean
)

class RefundManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope
) {

    fun refund(refundUri: String): LiveData<RefundStatus> {
        val liveData = MutableLiveData<RefundStatus>()
        scope.launch {
            api.request("applyRefund", RefundResponse.serializer()) {
                put("talerRefundUri", refundUri)
            }.onError {
                Log.e(TAG, "Refund Error: $it")
                // TODO show error string
                liveData.postValue(RefundStatus.Error)
            }.onSuccess {
                Log.e(TAG, "Refund Success: $it")
                liveData.postValue(RefundStatus.Success(it))
            }
        }
        return liveData
    }

}
