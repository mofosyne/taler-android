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
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.TAG
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

class ExchangeManager(
    private val walletBackendApi: WalletBackendApi,
    private val mapper: ObjectMapper
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    private val mExchanges = MutableLiveData<List<ExchangeItem>>()
    val exchanges: LiveData<List<ExchangeItem>> get() = list()

    private val mAddError = MutableLiveData<Event<Boolean>>()
    val addError: LiveData<Event<Boolean>> = mAddError

    fun add(exchangeUrl: String) {
        mProgress.value = true
        val args = JSONObject().apply { put("exchangeBaseUrl", exchangeUrl) }
        walletBackendApi.sendRequest("addExchange", args) { isError, result ->
            mProgress.value = false
            if (isError) {
                Log.e(TAG, "$result")
                mAddError.value = true.toEvent()
            } else {
                Log.d(TAG, "Exchange $exchangeUrl added")
                list()
            }
        }
    }

    private fun list(): LiveData<List<ExchangeItem>> {
        mProgress.value = true
        walletBackendApi.sendRequest("listExchanges", JSONObject()) { isError, result ->
            if (isError) {
                throw AssertionError("Wallet core failed to return exchanges!")
            } else {
                val exchanges: List<ExchangeItem> = mapper.readValue(result.getString("exchanges"))
                Log.d(TAG, "Exchange list: $exchanges")
                mProgress.value = false
                mExchanges.value = exchanges
            }
        }
        return mExchanges
    }

}
