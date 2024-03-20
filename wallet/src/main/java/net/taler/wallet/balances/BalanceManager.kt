/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.taler.common.CurrencySpecification
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import org.json.JSONObject

@Serializable
data class BalanceResponse(
    val balances: List<BalanceItem>
)

@Serializable
data class GetCurrencySpecificationResponse(
    val currencySpecification: CurrencySpecification,
)

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
    private val mBalances = MutableLiveData<List<BalanceItem>>(emptyList())
    val balances: LiveData<List<BalanceItem>> = mBalances

    private val mState = MutableLiveData<BalanceState>(BalanceState.None)
    val state: LiveData<BalanceState> = mState.distinctUntilChanged()

    private val currencySpecs: MutableMap<ScopeInfo, CurrencySpecification?> = mutableMapOf()

    @UiThread
    fun loadBalances() {
        mState.value = BalanceState.Loading
        scope.launch {
            val response = api.request("getBalances", BalanceResponse.serializer())
            response.onError {
                Log.e(TAG, "Error retrieving balances: $it")
                mState.postValue(BalanceState.Error(it))
            }
            response.onSuccess {
                mBalances.postValue(it.balances)
                scope.launch {
                    // Fetch missing currency specs for all balances
                    it.balances.forEach { balance ->
                        if (!currencySpecs.containsKey(balance.scopeInfo)) {
                            currencySpecs[balance.scopeInfo] = getCurrencySpecification(balance.scopeInfo)
                        }
                    }

                    mState.postValue(
                        BalanceState.Success(it.balances.map { balance ->
                            val spec = currencySpecs[balance.scopeInfo]
                            balance.copy(
                                available = balance.available.withSpec(spec),
                                pendingIncoming = balance.pendingIncoming.withSpec(spec),
                                pendingOutgoing = balance.pendingOutgoing.withSpec(spec),
                            )
                        }),
                    )
                }
            }
        }
    }

    private suspend fun getCurrencySpecification(scopeInfo: ScopeInfo): CurrencySpecification? {
        var spec: CurrencySpecification? = null
        api.request("getCurrencySpecification", GetCurrencySpecificationResponse.serializer()) {
            val json = Json.encodeToString(scopeInfo)
            Log.d(TAG, "BalanceManager: $json")
            put("scope", JSONObject(json))
        }.onSuccess {
            spec = it.currencySpecification
        }.onError {
            Log.e(TAG, "Error getting currency spec for scope $scopeInfo: $it")
        }

        return spec
    }

    @Deprecated("Please find spec via scopeInfo instead", ReplaceWith("getSpecForScopeInfo"))
    fun getSpecForCurrency(currency: String): CurrencySpecification? {
        val state = mState.value
        if (state !is BalanceState.Success) return null

        return state.balances.find { it.currency == currency }?.available?.spec
    }

    fun getSpecForScopeInfo(scopeInfo: ScopeInfo): CurrencySpecification? {
        val state = mState.value
        if (state !is BalanceState.Success) return null

        return state.balances.find { it.scopeInfo == scopeInfo }?.available?.spec
    }

    fun resetBalances() {
        mState.value = BalanceState.None
    }
}