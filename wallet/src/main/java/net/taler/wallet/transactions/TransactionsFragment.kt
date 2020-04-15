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
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_transactions.*
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

interface OnEventClickListener {
    fun onEventClicked(event: Transaction)
}

class TransactionsFragment : Fragment(), OnEventClickListener {

    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }
    private val transactionAdapter by lazy { TransactionAdapter(model.devMode.value == true, this) }
    private val currency by lazy { transactionManager.selectedCurrency!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = transactionAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        transactionManager.progress.observe(viewLifecycleOwner, Observer { show ->
            progressBar.visibility = if (show) VISIBLE else INVISIBLE
        })
        transactionManager.transactions.observe(viewLifecycleOwner, Observer { result ->
            onTransactionsResult(result)
        })

        // kicks off initial load, needs to be adapted if showAll state is ever saved
        if (savedInstanceState == null) transactionManager.showAll.value = model.devMode.value
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        model.balances.observe(viewLifecycleOwner, Observer { balances ->
            balances[currency]?.available?.let { amount ->
                requireActivity().title =
                    getString(R.string.transactions_detail_title_balance, amount)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onEventClicked(event: Transaction) {
        if (event.detailPageLayout != 0) {
            transactionManager.selectedEvent = event
            findNavController().navigate(R.id.action_nav_transactions_to_nav_transaction_detail)
        } else if (model.devMode.value == true) {
            JsonDialogFragment.new(event.json.toString(2))
                .show(parentFragmentManager, null)
        }
    }

    private fun onTransactionsResult(result: TransactionsResult) = when (result) {
        TransactionsResult.Error -> {
            list.fadeOut()
            emptyState.text = getString(R.string.transactions_error)
            emptyState.fadeIn()
        }
        is TransactionsResult.Success -> {
            emptyState.visibility = if (result.transactions.isEmpty()) VISIBLE else INVISIBLE
            transactionAdapter.update(result.transactions)
        }
    }

}
