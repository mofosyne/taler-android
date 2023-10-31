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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi

sealed class RefundStatus {
    data class Error(val error: TalerErrorInfo) : RefundStatus()
    data class Success(val response: StartRefundQueryForUriResponse) : RefundStatus()
}

@Serializable
data class StartRefundQueryForUriResponse(
    val transactionId: String,
)

class RefundManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope
) {

    fun refund(refundUri: String): LiveData<RefundStatus> {
        val liveData = MutableLiveData<RefundStatus>()
        scope.launch {
            api.request("startRefundQueryForUri", StartRefundQueryForUriResponse.serializer()) {
                put("talerRefundUri", refundUri)
            }.onError {
                liveData.postValue(RefundStatus.Error(it))
            }.onSuccess {
                liveData.postValue(RefundStatus.Success(it))
            }
        }
        return liveData
    }

}
