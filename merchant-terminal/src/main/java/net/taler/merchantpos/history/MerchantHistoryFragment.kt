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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_merchant_history.*
import net.taler.common.exhaustive
import net.taler.common.navigate
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.history.MerchantHistoryFragmentDirections.Companion.actionGlobalMerchantSettings
import net.taler.merchantpos.history.MerchantHistoryFragmentDirections.Companion.actionNavHistoryToRefundFragment

internal interface RefundClickListener {
    fun onRefundClicked(item: OrderHistoryEntry)
}

/**
 * Fragment to display the merchant's payment history, received from the backend.
 */
class MerchantHistoryFragment : Fragment(), RefundClickListener {

    companion object {
        const val TAG = "taler-merchant"
    }

    private val model: MainViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private val refundManager by lazy { model.refundManager }

    private val historyListAdapter = HistoryItemAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_merchant_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list_history.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
            adapter = historyListAdapter
        }

        swipeRefresh.setOnRefreshListener {
            Log.v(TAG, "refreshing!")
            historyManager.fetchHistory()
        }
        historyManager.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            Log.v(TAG, "setting refreshing to $loading")
            swipeRefresh.isRefreshing = loading
        })
        historyManager.items.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                is HistoryResult.Error -> onError(result.msg)
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

    private fun onError(msg: String) {
        Snackbar.make(requireView(), msg, LENGTH_LONG).show()
    }

    override fun onRefundClicked(item: OrderHistoryEntry) {
        refundManager.startRefund(item)
        navigate(actionNavHistoryToRefundFragment())
    }

}
