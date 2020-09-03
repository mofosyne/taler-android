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
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import net.taler.common.NfcManager.Companion.hasNfc
import net.taler.common.QrCodeManager.makeQrCode
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.databinding.FragmentProcessPaymentBinding
import net.taler.merchantpos.payment.ProcessPaymentFragmentDirections.Companion.actionProcessPaymentToPaymentSuccess
import net.taler.merchantpos.topSnackbar

class ProcessPaymentFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val paymentManager by lazy { model.paymentManager }

    private lateinit var ui: FragmentProcessPaymentBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentProcessPaymentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val introRes =
            if (hasNfc(requireContext())) R.string.payment_intro_nfc else R.string.payment_intro
        ui.payIntroView.setText(introRes)
        paymentManager.payment.observe(viewLifecycleOwner, { payment ->
            onPaymentStateChanged(payment)
        })
        ui.cancelPaymentButton.setOnClickListener {
            onPaymentCancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        paymentManager.cancelPayment(getString(R.string.error_cancelled))
    }

    private fun onPaymentStateChanged(payment: Payment) {
        if (payment.error != null) {
            topSnackbar(requireView(), payment.error, LENGTH_LONG)
            findNavController().navigateUp()
            return
        }
        if (payment.paid) {
            model.orderManager.onOrderPaid(payment.order.id)
            navigate(actionProcessPaymentToPaymentSuccess())
            return
        }
        ui.payIntroView.fadeIn()
        ui.amountView.text = payment.order.total.toString()
        payment.orderId?.let {
            ui.orderRefView.text = getString(R.string.payment_order_id, it)
            ui.orderRefView.fadeIn()
        }
        payment.talerPayUri?.let {
            val qrcodeBitmap = makeQrCode(it)
            ui.qrcodeView.setImageBitmap(qrcodeBitmap)
            ui.qrcodeView.fadeIn()
            ui.progressBar.fadeOut()
        }
    }

    private fun onPaymentCancel() {
        paymentManager.cancelPayment(getString(R.string.error_cancelled))
        findNavController().navigateUp()
        topSnackbar(requireView(), R.string.payment_canceled, LENGTH_LONG)
    }

}
