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

package net.taler.wallet.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_event_paid.*
import kotlinx.android.synthetic.main.fragment_event_withdraw.*
import kotlinx.android.synthetic.main.fragment_event_withdraw.feeView
import kotlinx.android.synthetic.main.fragment_event_withdraw.timeView
import net.taler.common.Amount
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.WalletViewModel
import net.taler.wallet.cleanExchange

class HistoryEventFragment : Fragment() {

    private val model: WalletViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private val event by lazy { requireNotNull(historyManager.selectedEvent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(model.devMode.value == true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(event.detailPageLayout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().title =
            getString(if (event.title != 0) event.title else R.string.history_detail_title)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        timeView.text = event.timestamp.ms.toAbsoluteTime(requireContext())
        when (val e = event) {
            is HistoryWithdrawnEvent -> bind(e)
            is HistoryPaymentSentEvent -> bind(e)
            is HistoryRefundedEvent -> bind(e)
            else -> Toast.makeText(
                requireContext(),
                "event ${e.javaClass} not implement",
                LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history_event, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.show_json -> {
                JsonDialogFragment.new(event.json.toString(2)).show(parentFragmentManager, null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun bind(event: HistoryWithdrawnEvent) {
        effectiveAmountLabel.text = getString(R.string.withdraw_total)
        effectiveAmountView.text = event.amountWithdrawnEffective.toString()
        chosenAmountLabel.text = getString(R.string.amount_chosen)
        chosenAmountView.text =
            getString(R.string.amount_positive, event.amountWithdrawnRaw.toString())
        val fee = event.amountWithdrawnRaw - event.amountWithdrawnEffective
        feeView.text = getString(R.string.amount_negative, fee.toString())
        exchangeView.text = cleanExchange(event.exchangeBaseUrl)
    }

    private fun bind(event: HistoryPaymentSentEvent) {
        amountPaidWithFeesView.text = event.amountPaidWithFees.toString()
        val fee = event.amountPaidWithFees - event.orderShortInfo.amount
        bindOrderAndFee(event.orderShortInfo, fee)
    }

    private fun bind(event: HistoryRefundedEvent) {
        amountPaidWithFeesLabel.text = getString(R.string.history_event_refund)
        amountPaidWithFeesView.setTextColor(getColor(requireContext(), R.color.green))
        amountPaidWithFeesView.text =
            getString(R.string.amount_positive, event.amountRefundedEffective.toString())
        val fee = event.orderShortInfo.amount - event.amountRefundedEffective
        bindOrderAndFee(event.orderShortInfo, fee)
    }

    private fun bindOrderAndFee(orderShortInfo: OrderShortInfo, fee: Amount) {
        orderAmountView.text = orderShortInfo.amount.toString()
        feeView.text = getString(R.string.amount_negative, fee.toString())
        orderSummaryView.text = orderShortInfo.summary
        orderIdView.text =
            getString(R.string.history_event_payment_sent_order_id, orderShortInfo.orderId)
    }

}