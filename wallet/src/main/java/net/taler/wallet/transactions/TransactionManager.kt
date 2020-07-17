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

package net.taler.wallet.transactions

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject
import java.util.HashMap
import java.util.LinkedList

sealed class TransactionsResult {
    object Error : TransactionsResult()
    class Success(val transactions: List<Transaction>) : TransactionsResult()
}

class TransactionManager(
    private val walletBackendApi: WalletBackendApi,
    private val scope: CoroutineScope,
    private val mapper: ObjectMapper
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    var selectedCurrency: String? = null
    var selectedTransaction: Transaction? = null

    val searchQuery = MutableLiveData<String>(null)
    private val allTransactions = HashMap<String, List<Transaction>>()
    private val mTransactions = HashMap<String, MutableLiveData<TransactionsResult>>()
    val transactions: LiveData<TransactionsResult>
        @UiThread
        get() = searchQuery.switchMap { query ->
            val currency = selectedCurrency
            check(currency != null) { "Did not select currency before getting transactions" }
            loadTransactions(query)
            mTransactions[currency]!! // non-null because filled in [loadTransactions]
        }

    @UiThread
    fun loadTransactions(searchQuery: String? = null) {
        val currency = selectedCurrency ?: return
        val liveData = mTransactions.getOrPut(currency) { MutableLiveData() }
        if (searchQuery == null && allTransactions.containsKey(currency)) {
            liveData.value = TransactionsResult.Success(allTransactions[currency]!!)
        }
        if (liveData.value == null) mProgress.value = true
        val request = JSONObject(mapOf("currency" to currency))
        searchQuery?.let { request.put("search", it) }
        walletBackendApi.sendRequest("getTransactions", request) { isError, result ->
            if (isError) {
                liveData.postValue(TransactionsResult.Error)
                mProgress.postValue(false)
            } else {
                val currencyToUpdate = if (searchQuery == null) currency else null
                scope.launch(Dispatchers.Default) {
                    onTransactionsLoaded(liveData, currencyToUpdate, result)
                }
            }
        }
    }

    @WorkerThread
    private fun onTransactionsLoaded(
        liveData: MutableLiveData<TransactionsResult>,
        currency: String?,  // only non-null if we should update all transactions cache
        result: JSONObject
    ) {
        val transactionsArray = result.getString("transactions")
        val transactions: LinkedList<Transaction> = mapper.readValue(transactionsArray)
        // TODO remove when fixed in wallet-core
        transactions.sortWith(compareBy({ it.pending }, { it.timestamp.ms }, { it.transactionId }))
        transactions.reverse()  // show latest first
        mProgress.postValue(false)
        liveData.postValue(TransactionsResult.Success(transactions))
        // update all transactions on UiThread if there was a currency
        currency?.let {
            scope.launch(Dispatchers.Main) { allTransactions[currency] = transactions }
        }
    }

    @UiThread
    fun hasPending(currency: String): Boolean {
        val result = mTransactions[currency]?.value ?: return false
        return if (result is TransactionsResult.Success) {
            result.transactions.any { it.pending }
        } else false
    }

}
