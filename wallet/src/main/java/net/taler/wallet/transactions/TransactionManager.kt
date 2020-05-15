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

import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
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

    private val mTransactions = HashMap<String, MutableLiveData<TransactionsResult>>()
    val transactions: LiveData<TransactionsResult>
        @UiThread
        get() {
            val currency = selectedCurrency
            check(currency != null) { "Did not select currency before getting transactions" }
            return mTransactions.getOrPut(currency) { MutableLiveData() }
        }

    @UiThread
    fun loadTransactions() {
        val currency = selectedCurrency ?: return
        val liveData = mTransactions.getOrPut(currency) {
            MutableLiveData<TransactionsResult>()
        }
        if (liveData.value == null) mProgress.value = true
        val request = JSONObject(mapOf("currency" to currency))
        walletBackendApi.sendRequest("getTransactions", request) { isError, result ->
            scope.launch(Dispatchers.Default) {
                onTransactionsLoaded(liveData, isError, result)
            }
        }
    }

    @WorkerThread
    private fun onTransactionsLoaded(
        liveData: MutableLiveData<TransactionsResult>,
        isError: Boolean,
        result: JSONObject
    ) {
        if (isError) {
            liveData.postValue(TransactionsResult.Error)
            return
        }
        Log.e("TEST", result.toString(2))  // TODO remove once API finalized
        val transactionsArray = result.getString("transactions")
        val transactions: LinkedList<Transaction> = mapper.readValue(transactionsArray)
        transactions.reverse()  // show latest first
        mProgress.postValue(false)
        liveData.postValue(TransactionsResult.Success(transactions))
    }

    @UiThread
    fun hasPending(currency: String): Boolean {
        val result = mTransactions[currency]?.value ?: return false
        return if (result is TransactionsResult.Success) {
            result.transactions.any { it.pending }
        } else false
    }

}
