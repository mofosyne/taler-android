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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_refund.*
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.fadeIn
import net.taler.merchantpos.fadeOut
import net.taler.merchantpos.history.RefundFragmentDirections.Companion.actionRefundFragmentToRefundUriFragment
import net.taler.merchantpos.history.RefundResult.Error
import net.taler.merchantpos.history.RefundResult.PastDeadline
import net.taler.merchantpos.history.RefundResult.Success
import net.taler.merchantpos.navigate

class RefundFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val refundManager by lazy { model.refundManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_refund, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val item = refundManager.toBeRefunded ?: throw IllegalStateException()
        amountInputView.setText(item.amount.amount)
        currencyView.text = item.amount.currency
        abortButton.setOnClickListener { findNavController().navigateUp() }
        refundButton.setOnClickListener { onRefundButtonClicked(item) }

        refundManager.refundResult.observe(viewLifecycleOwner, Observer { result ->
            onRefundResultChanged(result)
        })
    }

    private fun onRefundButtonClicked(item: HistoryItem) {
        val inputAmount = amountInputView.text.toString().toDouble()
        if (inputAmount > item.amount.amount.toDouble()) {
            amountView.error = getString(R.string.refund_error_max_amount, item.amount.amount)
            return
        }
        if (inputAmount <= 0.0) {
            amountView.error = getString(R.string.refund_error_zero)
            return
        }
        amountView.error = null
        refundButton.fadeOut()
        progressBar.fadeIn()
        refundManager.refund(item, inputAmount, reasonInputView.text.toString())
    }

    private fun onRefundResultChanged(result: RefundResult?): Any = when (result) {
        Error -> onError(R.string.refund_error_backend)
        PastDeadline -> onError(R.string.refund_error_deadline)
        is Success -> {
            progressBar.fadeOut()
            refundButton.fadeIn()
            navigate(actionRefundFragmentToRefundUriFragment())
        }
        null -> { // no-op
        }
    }

    private fun onError(@StringRes res: Int) {
        Snackbar.make(view!!, res, LENGTH_LONG).show()
        progressBar.fadeOut()
        refundButton.fadeIn()
    }

}
