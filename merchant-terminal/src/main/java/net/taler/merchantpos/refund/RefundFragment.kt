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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.AmountParserException
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.common.navigate
import net.taler.common.showError
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.databinding.FragmentRefundBinding
import net.taler.merchantpos.refund.RefundFragmentDirections.Companion.actionRefundFragmentToRefundUriFragment
import net.taler.merchantpos.refund.RefundResult.AlreadyRefunded
import net.taler.merchantpos.refund.RefundResult.Error
import net.taler.merchantpos.refund.RefundResult.PastDeadline
import net.taler.merchantpos.refund.RefundResult.Success

class RefundFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val refundManager by lazy { model.refundManager }

    private lateinit var ui: FragmentRefundBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentRefundBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val item = refundManager.toBeRefunded ?: throw IllegalStateException()
        ui.amountInputView.setText(item.amount.amountStr)
        ui.currencyView.text = item.amount.currency
        ui.abortButton.setOnClickListener { findNavController().navigateUp() }
        ui.refundButton.setOnClickListener { onRefundButtonClicked(item) }

        refundManager.refundResult.observe(viewLifecycleOwner, { result ->
            onRefundResultChanged(result)
        })
    }

    private fun onRefundButtonClicked(item: OrderHistoryEntry) {
        val inputAmount = try {
            Amount.fromString(item.amount.currency, ui.amountInputView.text.toString())
        } catch (e: AmountParserException) {
            ui.amountView.error = getString(R.string.refund_error_invalid_amount)
            return
        }
        if (inputAmount > item.amount) {
            ui.amountView.error = getString(R.string.refund_error_max_amount, item.amount.amountStr)
            return
        }
        if (inputAmount.isZero()) {
            ui.amountView.error = getString(R.string.refund_error_zero)
            return
        }
        ui.amountView.error = null
        ui.refundButton.fadeOut()
        ui.progressBar.fadeIn()
        refundManager.refund(item, inputAmount, ui.reasonInputView.text.toString())
    }

    private fun onRefundResultChanged(result: RefundResult?): Any = when (result) {
        is Error -> onError(R.string.refund_error_backend, result.msg)
        PastDeadline -> onError(R.string.refund_error_deadline)
        AlreadyRefunded -> onError(R.string.refund_error_already_refunded)
        is Success -> {
            ui.progressBar.fadeOut()
            ui.refundButton.fadeIn()
            navigate(actionRefundFragmentToRefundUriFragment())
        }
        null -> { // no-op
        }
    }

    private fun onError(@StringRes main: Int, details: String = "") {
        requireActivity().showError(main, details)
        ui.progressBar.fadeOut()
        ui.refundButton.fadeIn()
    }

}
