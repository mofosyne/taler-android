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
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.taler.merchantlib.CheckPaymentResponse
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.PostOrderResponse
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.order.Order
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS

private val TIMEOUT = MINUTES.toMillis(2)
private val CHECK_INTERVAL = SECONDS.toMillis(1)

class PaymentManager(
    private val context: Context,
    private val configManager: ConfigManager,
    private val scope: CoroutineScope,
    private val api: MerchantApi
) {

    private val mPayment = MutableLiveData<Payment>()
    val payment: LiveData<Payment> = mPayment

    private val checkTimer = object : CountDownTimer(TIMEOUT, CHECK_INTERVAL) {
        override fun onTick(millisUntilFinished: Long) {
            val orderId = payment.value?.orderId
            if (orderId == null) cancel()
            else checkPayment(orderId)
        }

        override fun onFinish() {
            val str = context.getString(R.string.error_timeout)
            payment.value?.copy(error = str)?.let { mPayment.value = it }
        }
    }

    @UiThread
    fun createPayment(order: Order) {
        val merchantConfig = configManager.merchantConfig!!
        mPayment.value = Payment(order, order.summary, merchantConfig.currency!!)
        scope.launch(Dispatchers.IO) {
            val response = api.postOrder(merchantConfig.convert(), order.toContractTerms())
            response.handle(::onNetworkError, ::onOrderCreated)
        }
    }

    private fun onOrderCreated(orderResponse: PostOrderResponse) = scope.launch(Dispatchers.Main) {
        mPayment.value = mPayment.value!!.copy(orderId = orderResponse.orderId)
        checkTimer.start()
    }

    private fun checkPayment(orderId: String) {
        val merchantConfig = configManager.merchantConfig!!
        scope.launch(Dispatchers.IO) {
            val response = api.checkOrder(merchantConfig.convert(), orderId)
            response.handle(::onNetworkError, ::onPaymentChecked)
        }
    }

    private fun onPaymentChecked(response: CheckPaymentResponse) = scope.launch(Dispatchers.Main) {
        val currentValue = requireNotNull(mPayment.value)
        if (response.paid) {
            mPayment.value = currentValue.copy(paid = true)
            checkTimer.cancel()
        } else if (currentValue.talerPayUri == null) {
            response as CheckPaymentResponse.Unpaid
            mPayment.value = currentValue.copy(talerPayUri = response.talerPayUri)
        }
    }

    private fun onNetworkError(error: String) = scope.launch(Dispatchers.Main) {
        cancelPayment(error)
    }

    fun cancelPayment(error: String) {
        mPayment.value = mPayment.value!!.copy(error = error)
        checkTimer.cancel()
    }

}
