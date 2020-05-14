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
import kotlinx.android.synthetic.main.fragment_transaction_payment.*
import kotlinx.android.synthetic.main.fragment_transaction_withdrawal.*
import kotlinx.android.synthetic.main.fragment_transaction_withdrawal.feeView
import kotlinx.android.synthetic.main.fragment_transaction_withdrawal.timeView
import net.taler.common.Amount
import net.taler.common.toAbsoluteTime
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange

class TransactionDetailFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }
    private val transaction by lazy { requireNotNull(transactionManager.selectedTransaction) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(model.devMode.value == true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(transaction.detailPageLayout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().apply {
            title = transaction.getTitle(this)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        timeView.text = transaction.timestamp.ms.toAbsoluteTime(requireContext())
        when (val e = transaction) {
            is TransactionWithdrawal -> bind(e)
            is TransactionPayment -> bind(e)
            is TransactionRefund -> bind(e)
            else -> Toast.makeText(
                requireContext(),
                "event ${e.javaClass} not implement",
                LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun bind(t: TransactionWithdrawal) {
        effectiveAmountLabel.text = getString(R.string.withdraw_total)
        effectiveAmountView.text = t.amountEffective.toString()
        chosenAmountLabel.text = getString(R.string.amount_chosen)
        chosenAmountView.text =
            getString(R.string.amount_positive, t.amountRaw.toString())
        val fee = t.amountRaw - (t.amountEffective ?: t.amountRaw)
        feeView.text = getString(R.string.amount_negative, fee.toString())
        exchangeView.text = cleanExchange(t.exchangeBaseUrl)
    }

    private fun bind(t: TransactionPayment) {
        amountPaidWithFeesView.text = t.amountEffective.toString()
        val fee = (t.amountEffective ?: t.amountRaw) - t.amountRaw
        bindOrderAndFee(t.info, t.amountRaw, fee)
    }

    private fun bind(t: TransactionRefund) {
        amountPaidWithFeesLabel.text = getString(R.string.transaction_refund)
        amountPaidWithFeesView.setTextColor(getColor(requireContext(), R.color.green))
        amountPaidWithFeesView.text =
            getString(R.string.amount_positive, t.amountEffective.toString())
        val fee = t.amountRaw - (t.amountEffective ?: t.amountRaw)
        bindOrderAndFee(t.info, t.amountRaw, fee)
    }

    private fun bindOrderAndFee(info: TransactionInfo, raw: Amount, fee: Amount) {
        orderAmountView.text = raw.toString()
        feeView.text = getString(R.string.amount_negative, fee.toString())
        orderSummaryView.text = info.summary
        orderIdView.text = getString(R.string.transaction_order_id, info.orderId)
    }

}
