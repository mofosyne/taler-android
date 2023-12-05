/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.wallet.balances

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi

@Serializable
sealed class BalanceState {
    data object None: BalanceState()
    data object Loading: BalanceState()

    data class Success(
        val balances: List<BalanceItem>,
    ): BalanceState()

    data class Error(
        val error: TalerErrorInfo,
    ): BalanceState()
}

class BalanceManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {
    private val mBalanceState = MutableLiveData<BalanceState>()
    val balanceState: LiveData<BalanceState> get() = mBalanceState.distinctUntilChanged()

    private val mError = MutableLiveData<TalerErrorInfo>()
    val error: LiveData<TalerErrorInfo> = mError

    fun listBalances(): Job = scope.launch {
        mBalanceState.value = BalanceState.Loading
        val balanceResponse = api.request("getBalances", BalanceResponse.serializer())

        balanceResponse.onError {
            Log.e(TAG, "Error retrieving balances: $it")
            mBalanceState.value = BalanceState.Error(it)
        }

        balanceResponse.onSuccess {
            mBalanceState.value = BalanceState.Success(it.balances)
        }
    }

    fun resetBalances() {
        mBalanceState.value = BalanceState.None
    }
}