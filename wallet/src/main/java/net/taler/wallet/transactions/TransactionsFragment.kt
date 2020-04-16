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
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_transactions.*
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

interface OnEventClickListener {
    fun onTransactionClicked(transaction: Transaction)
}

class TransactionsFragment : Fragment(), OnEventClickListener, ActionMode.Callback {

    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }

    private val transactionAdapter by lazy { TransactionAdapter(model.devMode.value == true, this) }
    private val currency by lazy { transactionManager.selectedCurrency!! }
    private var tracker: SelectionTracker<String>? = null
    private var actionMode: ActionMode? = null

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
            adapter = transactionAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        val tracker = SelectionTracker.Builder(
            "transaction-selection-id",
            list,
            transactionAdapter.keyProvider,
            TransactionLookup(list, transactionAdapter),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectAnything()
        ).build()
        savedInstanceState?.let { tracker.onRestoreInstanceState(it) }
        transactionAdapter.tracker = tracker
        this.tracker = tracker
        tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onItemStateChanged(key: String, selected: Boolean) {
                if (selected && actionMode == null) {
                    actionMode = requireActivity().startActionMode(this@TransactionsFragment)
                    updateActionModeTitle()
                } else if (actionMode != null) {
                    if (selected || tracker.hasSelection()) {
                        updateActionModeTitle()
                    } else {
                        actionMode!!.finish()
                    }
                }
            }
        })

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onTransactionClicked(transaction: Transaction) {
        if (actionMode != null) return // don't react on clicks while in action mode
        if (transaction.detailPageLayout != 0) {
            transactionManager.selectedEvent = transaction
            findNavController().navigate(R.id.action_nav_transaction_detail)
        } else if (model.devMode.value == true) {
            JsonDialogFragment.new(transaction.json.toString(2))
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
            list.fadeIn()
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val inflater = mode.menuInflater
        inflater.inflate(R.menu.transactions_action_mode, menu)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false // no update needed
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.transaction_delete -> {
                val s = "Not yet implemented. Pester Florian! ;)"
                Toast.makeText(requireContext(), s, LENGTH_LONG).show()
                mode.finish()
            }
            R.id.transaction_select_all -> transactionAdapter.selectAll()
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        tracker?.clearSelection()
        actionMode = null
    }

    private fun updateActionModeTitle() {
        tracker?.selection?.size()?.toString()?.let { num ->
            actionMode?.title = num
        }
    }

}
