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
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager.beginDelayedTransition
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import kotlinx.android.synthetic.main.payment_bottom_bar.*
import kotlinx.android.synthetic.main.payment_details.*
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

/**
 * Show a payment and ask the user to accept/decline.
 */
class PromptPaymentFragment : Fragment(), ProductImageClickListener {

    private val model: MainViewModel by activityViewModels()
    private val paymentManager by lazy { model.paymentManager }
    private val adapter = ProductAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prompt_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        paymentManager.payStatus.observe(viewLifecycleOwner, this::onPaymentStatusChanged)
        paymentManager.detailsShown.observe(viewLifecycleOwner, Observer { shown ->
            beginDelayedTransition(view as ViewGroup)
            val res = if (shown) R.string.payment_hide_details else R.string.payment_show_details
            detailsButton.setText(res)
            productsList.visibility = if (shown) VISIBLE else GONE
        })

        detailsButton.setOnClickListener {
            paymentManager.toggleDetailsShown()
        }
        productsList.apply {
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
            progressBar.fadeIn()
        } else {
            progressBar.fadeOut()
        }
    }

    private fun onPaymentStatusChanged(payStatus: PayStatus) {
        when (payStatus) {
            is PayStatus.Prepared -> {
                showLoading(false)
                val fees = payStatus.amountEffective - payStatus.amountRaw
                showOrder(payStatus.contractTerms, payStatus.amountRaw, fees)
                confirmButton.isEnabled = true
                confirmButton.setOnClickListener {
                    model.showProgressBar.value = true
                    paymentManager.confirmPay(
                        payStatus.proposalId,
                        payStatus.contractTerms.amount.currency
                    )
                    confirmButton.fadeOut()
                    confirmProgressBar.fadeIn()
                }
            }
            is PayStatus.InsufficientBalance -> {
                showLoading(false)
                showOrder(payStatus.contractTerms, payStatus.amountRaw)
                errorView.setText(R.string.payment_balance_insufficient)
                errorView.fadeIn()
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
                errorView.text = getString(R.string.payment_error, payStatus.error)
                errorView.fadeIn()
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

    private fun showOrder(contractTerms: ContractTerms, amount:Amount, totalFees: Amount? = null) {
        orderView.text = contractTerms.summary
        adapter.setItems(contractTerms.products)
        if (contractTerms.products.size == 1) paymentManager.toggleDetailsShown()
        totalView.text = amount.toString()
        if (totalFees != null && !totalFees.isZero()) {
            feeView.text = getString(R.string.payment_fee, totalFees)
            feeView.fadeIn()
        } else {
            feeView.visibility = GONE
        }
        orderLabelView.fadeIn()
        orderView.fadeIn()
        if (contractTerms.products.size > 1) detailsButton.fadeIn()
        totalLabelView.fadeIn()
        totalView.fadeIn()
    }

    override fun onImageClick(image: Bitmap) {
        val f = ProductImageFragment.new(image)
        f.show(parentFragmentManager, "image")
    }

}
