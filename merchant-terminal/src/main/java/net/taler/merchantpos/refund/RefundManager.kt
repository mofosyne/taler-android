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

package net.taler.merchantpos.refund

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.taler.common.Amount
import net.taler.common.assertUiThread
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantlib.RefundRequest
import net.taler.merchantpos.config.ConfigManager

sealed class RefundResult {
    class Error(val msg: String) : RefundResult()
    object PastDeadline : RefundResult()
    object AlreadyRefunded : RefundResult()
    class Success(
        val refundUri: String,
        val item: OrderHistoryEntry,
        val amount: Amount,
        val reason: String
    ) : RefundResult()
}

class RefundManager(
    private val configManager: ConfigManager,
    private val scope: CoroutineScope,
    private val api: MerchantApi
) {

    var toBeRefunded: OrderHistoryEntry? = null
        private set

    private val mRefundResult = MutableLiveData<RefundResult?>()
    internal val refundResult: LiveData<RefundResult?> = mRefundResult

    @UiThread
    internal fun startRefund(item: OrderHistoryEntry) {
        toBeRefunded = item
        mRefundResult.value = null
    }

    @UiThread
    internal fun abortRefund() {
        toBeRefunded = null
        mRefundResult.value = null
    }

    @UiThread
    internal fun refund(item: OrderHistoryEntry, amount: Amount, reason: String) = scope.launch {
        val merchantConfig = configManager.merchantConfig!!
        val request = RefundRequest(amount, reason)
        api.giveRefund(merchantConfig, item.orderId, request).handle(::onRefundError) {
            assertUiThread()
            mRefundResult.value = RefundResult.Success(
                refundUri = it.talerRefundUri,
                item = item,
                amount = amount,
                reason = reason
            )
        }
    }

    @UiThread
    private fun onRefundError(msg: String) {
        assertUiThread()
        if (msg.contains("2602")) {
            mRefundResult.postValue(RefundResult.AlreadyRefunded)
        } else mRefundResult.postValue(RefundResult.Error(msg))
    }
}
