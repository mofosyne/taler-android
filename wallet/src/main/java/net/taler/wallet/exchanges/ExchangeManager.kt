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
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.common.Event
import net.taler.common.toEvent
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi

@Serializable
data class ExchangeListResponse(
    val exchanges: List<ExchangeItem>,
)

@Serializable
data class ExchangeDetailedResponse(
    val exchange: ExchangeItem,
)

class ExchangeManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    private val mExchanges = MutableLiveData<List<ExchangeItem>>()
    val exchanges: LiveData<List<ExchangeItem>> get() = list()

    private val mAddError = MutableLiveData<Event<TalerErrorInfo>>()
    val addError: LiveData<Event<TalerErrorInfo>> = mAddError

    private val mListError = MutableLiveData<Event<TalerErrorInfo>>()
    val listError: LiveData<Event<TalerErrorInfo>> = mListError

    private val mDeleteError = MutableLiveData<Event<TalerErrorInfo>>()
    val deleteError: LiveData<Event<TalerErrorInfo>> = mDeleteError

    var withdrawalExchange: ExchangeItem? = null

    private fun list(): LiveData<List<ExchangeItem>> {
        mProgress.value = true
        scope.launch {
            val response = api.request("listExchanges", ExchangeListResponse.serializer())
            response.onError {
                mProgress.value = false
                mListError.value = it.toEvent()
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
            Log.e(TAG, "Error adding exchange: $it")
            mProgress.value = false
            mAddError.value = it.toEvent()
        }.onSuccess {
            mProgress.value = false
            Log.d(TAG, "Exchange $exchangeUrl added")
            list()
        }
    }

    fun delete(exchangeUrl: String, purge: Boolean = false) = scope.launch {
        mProgress.value = true
        api.request<Unit>("deleteExchange") {
            put("exchangeBaseUrl", exchangeUrl)
            put("purge", purge)
        }.onError {
            Log.e(TAG, "Error deleting exchange: $it")
            mProgress.value = false
            mDeleteError.value = it.toEvent()
        }.onSuccess {
            mProgress.value = false
            Log.d(TAG, "Exchange $exchangeUrl deleted")
            list()
        }
    }

    fun findExchangeForCurrency(currency: String): Flow<ExchangeItem?> = flow {
        emit(findExchange(currency))
    }

    @WorkerThread
    suspend fun findExchange(currency: String): ExchangeItem? {
        var exchange: ExchangeItem? = null
        api.request(
            operation = "listExchanges",
            serializer = ExchangeListResponse.serializer()
        ).onSuccess { exchangeListResponse ->
            // just pick the first for now
            exchange = exchangeListResponse.exchanges.find { it.currency == currency }
        }
        return exchange
    }

    @WorkerThread
    suspend fun findExchangeByUrl(exchangeUrl: String): ExchangeItem? {
        var exchange: ExchangeItem? = null
        api.request("getExchangeDetailedInfo", ExchangeDetailedResponse.serializer()) {
            put("exchangeBaseUrl", exchangeUrl)
        }.onError {
            Log.e(TAG, "Error getExchangeDetailedInfo: $it")
        }.onSuccess {
            exchange = it.exchange
        }
        return exchange
    }

    fun addDevExchanges() {
        scope.launch {
            listOf(
                "https://exchange.demo.taler.net/",
                "https://exchange.test.taler.net/",
                "https://exchange.head.taler.net/",
                "https://exchange.taler.ar/",
                "https://exchange.taler.fdold.eu/",
                "https://exchange.taler.grothoff.org/",
            ).forEach { exchangeUrl ->
                add(exchangeUrl)
                delay(100)
            }
            exchanges.value?.let { exs ->
                exs.find {
                    it.exchangeBaseUrl.startsWith("https://exchange.taler.fdold.eu")
                }?.let { fDoldExchange ->
                    api.request<Unit>("addGlobalCurrencyExchange") {
                        put("currency", fDoldExchange.currency)
                        put("exchangeBaseUrl", fDoldExchange.exchangeBaseUrl)
                        put("exchangeMasterPub",
                            "7ER30ZWJEXAG026H5KG9M19NGTFC2DKKFPV79GVXA6DK5DCNSWXG")
                    }.onError {
                        Log.e(TAG, "Error addGlobalCurrencyExchange: $it")
                    }.onSuccess {
                        Log.i(TAG, "fdold is global now!")
                    }
                }
            }
        }
    }

}
