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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.taler.common.isSafe
import net.taler.lib.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

abstract class TransactionDetailFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }
    protected val transaction: Transaction? get() = transactionManager.selectedTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(model.devMode.value == true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        requireActivity().apply {
            transaction?.generalTitleRes?.let {
                title = getString(it)
            }
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

    protected fun bindOrderAndFee(
        orderSummaryView: TextView,
        orderAmountView: TextView,
        orderIdView: TextView,
        feeView: TextView,
        info: TransactionInfo,
        raw: Amount,
        fee: Amount
    ) {
        orderAmountView.text = raw.toString()
        feeView.text = getString(R.string.amount_negative, fee.toString())
        orderSummaryView.text = if (info.fulfillmentMessage == null) {
            info.summary
        } else {
            "${info.summary}\n\n${info.fulfillmentMessage}"
        }
        if (info.fulfillmentUrl?.startsWith("http") == true) {
            val i = Intent().apply {
                data = Uri.parse(info.fulfillmentUrl)
            }
            if (i.isSafe(requireContext())) {
                orderSummaryView.setOnClickListener { startActivity(i) }
            }
        }
        orderIdView.text = getString(R.string.transaction_order_id, info.orderId)
    }

}
