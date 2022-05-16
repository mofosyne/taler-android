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

package net.taler.wallet.payment

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentPromptPaymentBinding

/**
 * Show a payment and ask the user to accept/decline.
 */
class PromptPaymentFragment : Fragment(), ProductImageClickListener {

    private val model: MainViewModel by activityViewModels()
    private val paymentManager by lazy { model.paymentManager }

    private lateinit var ui: FragmentPromptPaymentBinding
    private val adapter = ProductAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentPromptPaymentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        paymentManager.payStatus.observe(viewLifecycleOwner, ::onPaymentStatusChanged)

        ui.details.productsList.apply {
            adapter = this@PromptPaymentFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            paymentManager.abortPay()
        }
    }

    private fun showLoading(show: Boolean) {
        model.showProgressBar.value = show
        if (show) {
            ui.details.progressBar.fadeIn()
        } else {
            ui.details.progressBar.fadeOut()
        }
    }

    private fun onPaymentStatusChanged(payStatus: PayStatus?) {
        when (payStatus) {
            is PayStatus.Prepared -> {
                showLoading(false)
                val fees = payStatus.amountEffective - payStatus.amountRaw
                showOrder(payStatus.contractTerms, payStatus.amountRaw, fees)
                ui.bottom.confirmButton.isEnabled = true
                ui.bottom.confirmButton.setOnClickListener {
                    model.showProgressBar.value = true
                    paymentManager.confirmPay(
                        payStatus.proposalId,
                        payStatus.contractTerms.amount.currency
                    )
                    ui.bottom.confirmButton.fadeOut()
                    ui.bottom.confirmProgressBar.fadeIn()
                }
            }
            is PayStatus.InsufficientBalance -> {
                showLoading(false)
                showOrder(payStatus.contractTerms, payStatus.amountRaw)
                ui.details.errorView.setText(R.string.payment_balance_insufficient)
                ui.details.errorView.fadeIn()
            }
            is PayStatus.Success -> {
                showLoading(false)
                paymentManager.resetPayStatus()
                findNavController().navigate(R.id.action_promptPayment_to_nav_main)
                model.showTransactions(payStatus.currency)
                Snackbar.make(requireView(), R.string.payment_initiated, LENGTH_LONG).show()
            }
            is PayStatus.AlreadyPaid -> {
                showLoading(false)
                paymentManager.resetPayStatus()
                findNavController().navigate(R.id.action_promptPayment_to_alreadyPaid)
            }
            is PayStatus.Error -> {
                showLoading(false)
                ui.details.errorView.text = getString(R.string.payment_error, payStatus.error)
                ui.details.errorView.fadeIn()
            }
            is PayStatus.None -> {
                // No payment active.
                showLoading(false)
            }
            is PayStatus.Loading -> {
                // Wait until loaded ...
                showLoading(true)
            }
        }
    }

    private fun showOrder(contractTerms: ContractTerms, amount: Amount, totalFees: Amount? = null) {
        ui.details.orderView.text = contractTerms.summary
        adapter.setItems(contractTerms.products)
        ui.details.productsList.fadeIn()
        ui.bottom.totalView.text = amount.toString()
        if (totalFees != null && !totalFees.isZero()) {
            ui.bottom.feeView.text = getString(R.string.payment_fee, totalFees)
            ui.bottom.feeView.fadeIn()
        } else {
            ui.bottom.feeView.visibility = GONE
        }
        ui.details.orderLabelView.fadeIn()
        ui.details.orderView.fadeIn()
        ui.bottom.totalLabelView.fadeIn()
        ui.bottom.totalView.fadeIn()
    }

    override fun onImageClick(image: Bitmap) {
        val f = ProductImageFragment.new(image)
        f.show(parentFragmentManager, "image")
    }

}
