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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import net.taler.wallet.backend.WalletBackendApi

sealed class TransactionsResult {
    object Error : TransactionsResult()
    class Success(val transactions: Transactions) : TransactionsResult()
}

@Suppress("EXPERIMENTAL_API_USAGE")
class TransactionManager(
    private val walletBackendApi: WalletBackendApi,
    private val mapper: ObjectMapper
) {

    private val mProgress = MutableLiveData<Boolean>()
    val progress: LiveData<Boolean> = mProgress

    val showAll = MutableLiveData<Boolean>()

    var selectedCurrency: String? = null
    var selectedEvent: Transaction? = null

    val transactions: LiveData<TransactionsResult> = showAll.switchMap { showAll ->
        loadTransactions(showAll)
            .onStart { mProgress.postValue(true) }
            .onCompletion { mProgress.postValue(false) }
            .asLiveData(Dispatchers.IO)
    }

    private fun loadTransactions(showAll: Boolean) = callbackFlow {
        walletBackendApi.sendRequest("getHistory", null) { isError, result ->
            launch(Dispatchers.Default) {
                if (isError) {
                    offer(TransactionsResult.Error)
                    close()
                    return@launch
                }
                val transactions = Transactions()
                val json = result.getJSONArray("history")
                val currency = selectedCurrency
                for (i in 0 until json.length()) {
                    val event: Transaction = mapper.readValue(json.getString(i))
                    event.json = json.getJSONObject(i)
                    if (currency == null || event.isCurrency(currency)) {
                        transactions.add(event)
                    }
                }
                transactions.reverse()  // show latest first
                val filtered =
                    if (showAll) transactions else transactions.filter { it.showToUser } as Transactions
                offer(TransactionsResult.Success(filtered))
                close()
            }
        }
        awaitClose()
    }

}
