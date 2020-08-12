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

package net.taler.wallet.exchanges

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi

@Serializable
data class ExchangeListResponse(
    val exchanges: List<ExchangeItem>
)

class ExchangeManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    private val mExchanges = MutableLiveData<List<ExchangeItem>>()
    val exchanges: LiveData<List<ExchangeItem>> get() = list()

    private val mAddError = MutableLiveData<Event<Boolean>>()
    val addError: LiveData<Event<Boolean>> = mAddError

    var withdrawalExchange: ExchangeItem? = null

    private fun list(): LiveData<List<ExchangeItem>> {
        mProgress.value = true
        scope.launch {
            val response = api.request("listExchanges", ExchangeListResponse.serializer())
            response.onError {
                throw AssertionError("Wallet core failed to return exchanges!")
            }.onSuccess {
                Log.d(TAG, "Exchange list: ${it.exchanges}")
                mProgress.value = false
                mExchanges.value = it.exchanges
            }
        }
        return mExchanges
    }

    fun add(exchangeUrl: String) = scope.launch {
        mProgress.value = true
        api.request<Unit>("addExchange") {
            put("exchangeBaseUrl", exchangeUrl)
        }.onError {
            mProgress.value = false
            Log.e(TAG, "Error adding exchange: $it")
            mAddError.value = true.toEvent()
        }.onSuccess {
            mProgress.value = false
            Log.d(TAG, "Exchange $exchangeUrl added")
            list()
        }
    }

}
