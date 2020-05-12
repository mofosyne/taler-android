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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.history.History
import net.taler.wallet.history.HistoryEvent
import org.json.JSONObject

sealed class TransactionsResult {
    object Error : TransactionsResult()
    class Success(val transactions: History) : TransactionsResult()
}

class TransactionManager(
    private val walletBackendApi: WalletBackendApi,
    private val scope: CoroutineScope,
    private val mapper: ObjectMapper
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    var selectedCurrency: String? = null
    var selectedEvent: HistoryEvent? = null

    private val mTransactions = MutableLiveData<TransactionsResult>()
    val transactions: LiveData<TransactionsResult> = mTransactions

    fun loadTransactions() {
        mProgress.postValue(true)
        walletBackendApi.sendRequest("getHistory", null) { isError, result ->
            scope.launch(Dispatchers.Default) {
                onTransactionsLoaded(isError, result)
            }
        }
    }

    private fun onTransactionsLoaded(isError: Boolean, result: JSONObject) {
        if (isError) {
            mTransactions.postValue(TransactionsResult.Error)
            return
        }
        val transactions = History()
        val json = result.getJSONArray("history")
        val currency = selectedCurrency
        for (i in 0 until json.length()) {
            val event: HistoryEvent = mapper.readValue(json.getString(i))
            if (event.showToUser && (currency == null || event.isCurrency(currency))) {
                transactions.add(event)
            }
        }
        transactions.reverse()  // show latest first
        mProgress.postValue(false)
        mTransactions.postValue(TransactionsResult.Success(transactions))
    }

}
