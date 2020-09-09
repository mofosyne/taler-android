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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import net.taler.common.exhaustive
import net.taler.common.navigate
import net.taler.common.showError
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.databinding.FragmentMerchantHistoryBinding
import net.taler.merchantpos.history.HistoryFragmentDirections.Companion.actionGlobalMerchantSettings
import net.taler.merchantpos.history.HistoryFragmentDirections.Companion.actionNavHistoryToRefundFragment

internal interface RefundClickListener {
    fun onRefundClicked(item: OrderHistoryEntry)
}

/**
 * Fragment to display the merchant's payment history, received from the backend.
 */
class HistoryFragment : Fragment(), RefundClickListener {

    companion object {
        const val TAG = "taler-merchant"
    }

    private val model: MainViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private val refundManager by lazy { model.refundManager }

    private lateinit var ui: FragmentMerchantHistoryBinding
    private val historyListAdapter = HistoryItemAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentMerchantHistoryBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.listHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
            adapter = historyListAdapter
        }

        ui.swipeRefresh.setOnRefreshListener {
            Log.v(TAG, "refreshing!")
            historyManager.fetchHistory()
        }
        historyManager.isLoading.observe(viewLifecycleOwner, { loading ->
            Log.v(TAG, "setting refreshing to $loading")
            ui.swipeRefresh.isRefreshing = loading
        })
        historyManager.items.observe(viewLifecycleOwner, { result ->
            when (result) {
                is HistoryResult.Error -> requireActivity().showError(R.string.error_history, result.msg)
                is HistoryResult.Success -> historyListAdapter.setData(result.items)
            }.exhaustive
        })
    }

    override fun onStart() {
        super.onStart()
        if (model.configManager.merchantConfig?.baseUrl == null) {
            navigate(actionGlobalMerchantSettings())
        } else {
            historyManager.fetchHistory()
        }
    }

    override fun onRefundClicked(item: OrderHistoryEntry) {
        refundManager.startRefund(item)
        navigate(actionNavHistoryToRefundFragment())
    }

}
