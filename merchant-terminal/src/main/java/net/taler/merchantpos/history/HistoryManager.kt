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

package net.taler.merchantpos.history

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.common.assertUiThread
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.config.ConfigManager

sealed class HistoryResult {
    class Error(val msg: String) : HistoryResult()
    class Success(val items: List<OrderHistoryEntry>) : HistoryResult()
}

class HistoryManager(
    private val configManager: ConfigManager,
    private val scope: CoroutineScope,
    private val api: MerchantApi
) {

    private val mIsLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = mIsLoading

    private val mItems = MutableLiveData<HistoryResult>()
    val items: LiveData<HistoryResult> = mItems

    @UiThread
    internal fun fetchHistory() = scope.launch {
        mIsLoading.value = true
        val merchantConfig = configManager.merchantConfig!!
        api.getOrderHistory(merchantConfig).handle(::onHistoryError) {
            assertUiThread()
            mIsLoading.value = false
            mItems.value = HistoryResult.Success(it.orders)
        }
    }

    private fun onHistoryError(msg: String) {
        assertUiThread()
        mIsLoading.value = false
        mItems.value = HistoryResult.Error(msg)
    }
}
