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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import kotlinx.android.synthetic.main.fragment_process_payment.*
import net.taler.common.NfcManager.Companion.hasNfc
import net.taler.common.QrCodeManager.makeQrCode
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.payment.ProcessPaymentFragmentDirections.Companion.actionProcessPaymentToPaymentSuccess
import net.taler.merchantpos.topSnackbar

class ProcessPaymentFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val paymentManager by lazy { model.paymentManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_process_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val introRes =
            if (hasNfc(requireContext())) R.string.payment_intro_nfc else R.string.payment_intro
        payIntroView.setText(introRes)
        paymentManager.payment.observe(viewLifecycleOwner, Observer { payment ->
            onPaymentStateChanged(payment)
        })
        cancelPaymentButton.setOnClickListener {
            onPaymentCancel()
        }
    }

    private fun onPaymentStateChanged(payment: Payment) {
        if (payment.error) {
            topSnackbar(view!!, R.string.error_network, LENGTH_LONG)
            findNavController().navigateUp()
            return
        }
        if (payment.paid) {
            model.orderManager.onOrderPaid(payment.order.id)
            navigate(actionProcessPaymentToPaymentSuccess())
            return
        }
        payIntroView.fadeIn()
        amountView.text = payment.order.total.toString()
        payment.orderId?.let {
            orderRefView.text = getString(R.string.payment_order_ref, it)
            orderRefView.fadeIn()
        }
        payment.talerPayUri?.let {
            val qrcodeBitmap = makeQrCode(it)
            qrcodeView.setImageBitmap(qrcodeBitmap)
            qrcodeView.fadeIn()
            progressBar.fadeOut()
        }
    }

    private fun onPaymentCancel() {
        paymentManager.cancelPayment()
        findNavController().navigateUp()
        topSnackbar(view!!, R.string.payment_canceled, LENGTH_LONG)
    }

}
