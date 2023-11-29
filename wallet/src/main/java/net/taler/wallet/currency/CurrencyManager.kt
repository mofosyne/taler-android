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

package net.taler.wallet.currency

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.balances.ScopeInfo

@Serializable
data class GetCurrencySpecificationResponse(
    val currencySpecification: CurrencySpecification,
)

class CurrencyManager(
    private val api: WalletBackendApi,
    private val scope: CoroutineScope,
) {
    private val mCurrencyInfo = MutableLiveData<CurrencyInfo>()
    val currencyInfo: LiveData<CurrencyInfo> get() = listCurrencies()

    private val mError = MutableLiveData<TalerErrorInfo>()
    val error: LiveData<TalerErrorInfo> = mError

    private fun listCurrencies(): LiveData<CurrencyInfo> {
        scope.launch {
            val response = api.request("listCurrencies", CurrencyInfo.serializer())
            response.onError {
                mError.value = it
            }.onSuccess {
                Log.d(TAG, "Currency info: $it")
                mCurrencyInfo.value = it
            }
        }
        return mCurrencyInfo
    }

    suspend fun getCurrencySpecification(scopeInfo: ScopeInfo): CurrencySpecification? {
        var spec: CurrencySpecification? = null
        api.request("getCurrencySpecification", GetCurrencySpecificationResponse.serializer())
            .onSuccess {
                spec = it.currencySpecification
            }
        return spec
    }
}