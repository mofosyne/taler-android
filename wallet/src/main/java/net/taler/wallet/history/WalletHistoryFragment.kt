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
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_show_history.*
import net.taler.wallet.R
import net.taler.wallet.WalletViewModel

interface OnEventClickListener {
    fun onEventClicked(event: HistoryEvent)
}

class WalletHistoryFragment : Fragment(), OnEventClickListener {

    private val model: WalletViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private lateinit var showAllItem: MenuItem
    private var reloadHistoryItem: MenuItem? = null
    private val historyAdapter = WalletHistoryAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_show_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        historyList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        model.devMode.observe(viewLifecycleOwner, Observer { enabled ->
            reloadHistoryItem?.isVisible = enabled
        })
        historyManager.progress.observe(viewLifecycleOwner, Observer { show ->
            historyProgressBar.visibility = if (show) VISIBLE else INVISIBLE
        })
        historyManager.history.observe(viewLifecycleOwner, Observer { history ->
            historyEmptyState.visibility = if (history.isEmpty()) VISIBLE else INVISIBLE
            historyAdapter.update(history)
        })

        // kicks off initial load, needs to be adapted if showAll state is ever saved
        if (savedInstanceState == null) historyManager.showAll.value = model.devMode.value
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history, menu)
        showAllItem = menu.findItem(R.id.show_all_history)
        showAllItem.isChecked = historyManager.showAll.value == true
        reloadHistoryItem = menu.findItem(R.id.reload_history).apply {
            isVisible = model.devMode.value!!
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.show_all_history -> {
                item.isChecked = !item.isChecked
                historyManager.showAll.value = item.isChecked
                true
            }
            R.id.reload_history -> {
                historyManager.showAll.value = showAllItem.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onEventClicked(event: HistoryEvent) {
        if (model.devMode.value != true) return
        JsonDialogFragment.new(event.json.toString(4))
            .show(parentFragmentManager, null)
    }

}
