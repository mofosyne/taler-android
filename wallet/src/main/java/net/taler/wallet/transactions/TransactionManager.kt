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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.transactions.TransactionAction.Delete
import net.taler.wallet.transactions.TransactionMajorState.Pending
import java.util.LinkedList

sealed class TransactionsResult {
    class Error(val error: TalerErrorInfo) : TransactionsResult()
    class Success(val transactions: List<Transaction>) : TransactionsResult()
}

class TransactionManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    // FIXME if the app gets killed, this will not be restored and thus be unexpected null
    //  we should keep this in a savable, maybe using Hilt and SavedStateViewModel
    var selectedCurrency: String? = null

    val searchQuery = MutableLiveData<String>(null)
    private val mSelectedTransaction = MutableLiveData<Transaction?>(null)
    val selectedTransaction: LiveData<Transaction?> = mSelectedTransaction
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
    fun loadTransactions(searchQuery: String? = null) = scope.launch {
        val currency = selectedCurrency ?: return@launch
        val liveData = mTransactions.getOrPut(currency) { MutableLiveData() }
        if (searchQuery == null && allTransactions.containsKey(currency)) {
            liveData.value = TransactionsResult.Success(allTransactions[currency]!!)
        }
        if (liveData.value == null) mProgress.value = true

        api.request("getTransactions", Transactions.serializer()) {
            if (searchQuery != null) put("search", searchQuery)
            put("currency", currency)
        }.onError {
            liveData.postValue(TransactionsResult.Error(it))
            mProgress.postValue(false)
        }.onSuccess { result ->
            val transactions = LinkedList(result.transactions)
            transactions.reverse() // show latest first

            mProgress.value = false
            liveData.value = TransactionsResult.Success(transactions)

            // update selected transaction on UiThread (if it exists)
            val selected = selectedTransaction.value
            if (selected != null) transactions.find {
                it.transactionId == selected.transactionId
            }?.let {
                mSelectedTransaction.value = it
            }

            // update all transactions on UiThread if there was a currency
            if (searchQuery == null) allTransactions[currency] = transactions
        }
    }

    /**
     * Returns true if given [transactionId] was found and selected, false otherwise.
     */
    @UiThread
    suspend fun selectTransaction(transactionId: String): Boolean {
        var transaction: Transaction? = null
        api.request("getTransactionById", Transaction.serializer()) {
            put("transactionId", transactionId)
        }.onError {
            Log.e(TAG, "Error getting transaction $it")
        }.onSuccess { result ->
            transaction = result
        }
        return if (transaction != null) {
            mSelectedTransaction.value = transaction
            true
        } else {
            false
        }
    }

    suspend fun getTransactionById(transactionId: String): Transaction? {
        var transaction: Transaction? = null
        api.request("getTransactionById", Transaction.serializer()) {
            put("transactionId", transactionId)
        }.onError {
            Log.e(TAG, "Error getting transaction $it")
        }.onSuccess { result ->
            transaction = result
        }
        return transaction
    }

    fun selectTransaction(transaction: Transaction) {
        mSelectedTransaction.postValue(transaction)
    }

    fun deleteTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("deleteTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                // re-load transactions as our list is stale otherwise
                loadTransactions()
            }
        }

    fun retryTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("retryTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                loadTransactions()
            }
        }

    fun abortTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("abortTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                loadTransactions()
            }
        }

    fun failTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("failTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                loadTransactions()
            }
        }

    fun suspendTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("suspendTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                loadTransactions()
            }
        }

    fun resumeTransaction(transactionId: String, onError: (it: TalerErrorInfo) -> Unit) =
        scope.launch {
            api.request<Unit>("resumeTransaction") {
                put("transactionId", transactionId)
            }.onError {
                onError(it)
            }.onSuccess {
                loadTransactions()
            }
        }

    fun deleteTransactions(transactionIds: List<String>, onError: (it: TalerErrorInfo) -> Unit) {
        allTransactions[selectedCurrency]?.filter { transaction ->
            transaction.transactionId in transactionIds
        }?.forEach { toBeDeletedTx ->
            if (Delete in toBeDeletedTx.txActions) {
                deleteTransaction(toBeDeletedTx.transactionId) {
                    onError(it)
                }
            }
        }
    }

}
