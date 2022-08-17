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
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import net.taler.common.Amount
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentTransactionsBinding

interface OnTransactionClickListener {
    fun onTransactionClicked(transaction: Transaction)
}

class TransactionsFragment : Fragment(), OnTransactionClickListener, ActionMode.Callback {

    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }

    private lateinit var ui: FragmentTransactionsBinding
    private val transactionAdapter by lazy { TransactionAdapter(this) }
    private val currency by lazy { transactionManager.selectedCurrency!! }
    private var tracker: SelectionTracker<String>? = null
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentTransactionsBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.list.apply {
            adapter = transactionAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        val tracker = SelectionTracker.Builder(
            "transaction-selection-id",
            ui.list,
            transactionAdapter.keyProvider,
            TransactionLookup(ui.list, transactionAdapter),
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

        model.balances.observe(viewLifecycleOwner) { balances ->
            // hide extra fab when in single currency mode (uses MainFragment's FAB)
            if (balances.size == 1) ui.mainFab.visibility = INVISIBLE
            balances.find { it.currency == currency }?.available?.let { amount: Amount ->
                requireActivity().title =
                    getString(R.string.transactions_detail_title_balance, amount)
            }
        }
        transactionManager.progress.observe(viewLifecycleOwner) { show ->
            if (show) ui.progressBar.fadeIn() else ui.progressBar.fadeOut()
        }
        transactionManager.transactions.observe(viewLifecycleOwner) { result ->
            onTransactionsResult(result)
        }
        ui.mainFab.setOnClickListener {
            model.scanCode()
        }
        ui.mainFab.setOnLongClickListener {
            findNavController().navigate(R.id.action_nav_transactions_to_nav_uri_input)
            true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions, menu)
        setupSearch(menu.findItem(R.id.action_search))
    }

    private fun setupSearch(item: MenuItem) {
        item.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem) = true
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                onSearchClosed()
                return true
            }
        })
        val searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextChange(newText: String) = false
            override fun onQueryTextSubmit(query: String): Boolean {
                // workaround to avoid issues with some emulators and keyboard devices
                // firing twice if a keyboard enter is used
                // see https://code.google.com/p/android/issues/detail?id=24599
                item.actionView.clearFocus()
                onSearch(query)
                return true
            }
        })
    }

    override fun onTransactionClicked(transaction: Transaction) {
        if (actionMode != null) return // don't react on clicks while in action mode
        if (transaction.detailPageNav != 0) {
            transactionManager.selectedTransaction = transaction
            findNavController().navigate(transaction.detailPageNav)
        }
    }

    private fun onTransactionsResult(result: TransactionsResult) = when (result) {
        is TransactionsResult.Error -> {
            ui.list.fadeOut()
            ui.emptyState.text = getString(R.string.transactions_error, result.msg)
            ui.emptyState.fadeIn()
        }
        is TransactionsResult.Success -> {
            if (result.transactions.isEmpty()) {
                val isSearch = transactionManager.searchQuery.value != null
                ui.emptyState.setText(if (isSearch) R.string.transactions_empty_search else R.string.transactions_empty)
                ui.emptyState.fadeIn()
                ui.list.fadeOut()
            } else {
                ui.emptyState.fadeOut()
                transactionAdapter.update(result.transactions)
                ui.list.fadeIn()
            }
        }
    }

    private fun onSearch(query: String) {
        ui.list.fadeOut()
        ui.progressBar.fadeIn()
        transactionManager.searchQuery.value = query
    }

    private fun onSearchClosed() {
        transactionManager.searchQuery.value = null
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
                tracker?.selection?.toList()?.let { transactionIds ->
                    transactionManager.deleteTransactions(transactionIds)
                }
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
