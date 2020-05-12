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
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_transactions.*
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

internal interface OnEventClickListener {
    fun onTransactionClicked(historyEvent: HistoryEvent)
}

class DevHistoryFragment : Fragment(),
    OnEventClickListener {

    private val model: MainViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private val historyAdapter by lazy { DevHistoryAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) historyManager.loadHistory()

        list.apply {
            adapter = historyAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        historyManager.progress.observe(viewLifecycleOwner, Observer { show ->
            progressBar.visibility = if (show) VISIBLE else INVISIBLE
        })
        historyManager.history.observe(viewLifecycleOwner, Observer { result ->
            onHistoryResult(result)
        })
    }

    override fun onTransactionClicked(historyEvent: HistoryEvent) {
        JsonDialogFragment.new(historyEvent.json.toString(2))
            .show(parentFragmentManager, null)
    }

    private fun onHistoryResult(result: HistoryResult) = when (result) {
        HistoryResult.Error -> {
            list.fadeOut()
            emptyState.text = getString(R.string.transactions_error)
            emptyState.fadeIn()
        }
        is HistoryResult.Success -> {
            emptyState.visibility = if (result.history.isEmpty()) VISIBLE else INVISIBLE
            historyAdapter.update(result.history)
            list.fadeIn()
        }
    }

}
