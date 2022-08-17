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

package net.taler.wallet.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentTransactionPaymentBinding

class TransactionRefundFragment : TransactionDetailFragment() {

    private lateinit var ui: FragmentTransactionPaymentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentTransactionPaymentBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val t = transaction as TransactionRefund
        ui.timeView.text = t.timestamp.ms.toAbsoluteTime(requireContext())

        ui.amountPaidWithFeesLabel.text = getString(R.string.transaction_refund)
        ui.amountPaidWithFeesView.setTextColor(getColor(requireContext(), R.color.green))
        ui.amountPaidWithFeesView.text =
            getString(R.string.amount_positive, t.amountEffective.toString())
        val fee = t.amountRaw - t.amountEffective
        bindOrderAndFee(
            ui.orderSummaryView,
            ui.orderAmountView,
            ui.orderIdView,
            ui.feeView,
            t.info,
            t.amountRaw,
            fee
        )
        ui.deleteButton.setOnClickListener {
            onDeleteButtonClicked(t)
        }
    }

}
