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

package net.taler.merchantpos.payment

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.taler.common.RelativeTime
import net.taler.common.assertUiThread
import net.taler.merchantlib.CheckPaymentResponse
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.PostOrderRequest
import net.taler.merchantpos.MainActivity.Companion.TAG
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.order.Order
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.SECONDS

private const val TIMEOUT = Long.MAX_VALUE
private val CHECK_INTERVAL = SECONDS.toMillis(1)

class PaymentManager(
    private val context: Context,
    private val configManager: ConfigManager,
    private val scope: CoroutineScope,
    private val api: MerchantApi,
) {

    private val mPayment = MutableLiveData<Payment>()
    val payment: LiveData<Payment> = mPayment
    private var checkJob: Job? = null

    private val checkTimer: CountDownTimer = object : CountDownTimer(TIMEOUT, CHECK_INTERVAL) {
        override fun onTick(millisUntilFinished: Long) {
            val orderId = payment.value?.orderId
            if (orderId == null) cancel()
            // only start new job if old one doesn't exist or is complete
            else if (checkJob == null || checkJob?.isCompleted == true) {
                checkJob = checkPayment(orderId)
            }
        }

        override fun onFinish() {
            cancelPayment(context.getString(R.string.error_timeout))
        }
    }

    @UiThread
    fun createPayment(order: Order) = scope.launch {
        val merchantConfig = configManager.merchantConfig!!
        mPayment.value = Payment(order, order.summary, configManager.currency!!)
        val request = PostOrderRequest(
            contractTerms = order.toContractTerms(),
            refundDelay = RelativeTime.fromMillis(HOURS.toMillis(1))
        )
        api.postOrder(merchantConfig, request).handle(::onNetworkError) { orderResponse ->
            assertUiThread()
            mPayment.value = mPayment.value!!.copy(orderId = orderResponse.orderId)
            checkTimer.start()
        }
    }

    private fun checkPayment(orderId: String) = scope.launch {
        val merchantConfig = configManager.merchantConfig!!
        api.checkOrder(merchantConfig, orderId).handle({ error ->
            // don't call onNetworkError() to not cancel payment, just keep trying
            Log.d(TAG, "Network error: $error")
        }) { response ->
            assertUiThread()
            if (!isActive) return@handle // don't continue if job was cancelled
            val currentValue = requireNotNull(mPayment.value)
            when (response) {
                is CheckPaymentResponse.Unpaid -> {
                    mPayment.value = currentValue.copy(talerPayUri = response.talerPayUri)
                }
                is CheckPaymentResponse.Claimed -> {
                    mPayment.value = currentValue.copy(claimed = true)
                }
                is CheckPaymentResponse.Paid -> {
                    mPayment.value = currentValue.copy(paid = true)
                    checkTimer.cancel()
                }
            }
        }
    }

    private fun onNetworkError(error: String) {
        assertUiThread()
        Log.d(TAG, "Network error: $error")
        cancelPayment(error)
    }

    @UiThread
    fun cancelPayment(error: String) {
        // delete unpaid order
        val merchantConfig = configManager.merchantConfig!!
        mPayment.value?.let { payment ->
            if (!payment.paid && payment.error != null) payment.orderId?.let { orderId ->
                Log.d(TAG, "Deleting cancelled and unpaid order $orderId")
                scope.launch {
                    api.deleteOrder(merchantConfig, orderId)
                }
            }
        }
        mPayment.value?.copy(error = error)?.let {
            mPayment.value = it
        }
        checkTimer.cancel()
        checkJob?.isCancelled
        checkJob = null
    }
}
