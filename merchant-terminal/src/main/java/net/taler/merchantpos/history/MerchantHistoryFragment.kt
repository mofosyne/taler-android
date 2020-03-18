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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import kotlinx.android.synthetic.main.fragment_merchant_history.*
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.exhaustive
import net.taler.merchantpos.history.HistoryItemAdapter.HistoryItemViewHolder
import net.taler.merchantpos.history.MerchantHistoryFragmentDirections.Companion.actionGlobalMerchantSettings
import net.taler.merchantpos.history.MerchantHistoryFragmentDirections.Companion.actionNavHistoryToRefundFragment
import net.taler.merchantpos.navigate
import net.taler.merchantpos.toRelativeTime
import java.util.*

private interface RefundClickListener {
    fun onRefundClicked(item: HistoryItem)
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
                is HistoryResult.Error -> onError()
                is HistoryResult.Success -> historyListAdapter.setData(result.items)
            }.exhaustive
        })
    }

    override fun onStart() {
        super.onStart()
        if (model.configManager.merchantConfig?.instance == null) {
            navigate(actionGlobalMerchantSettings())
        } else {
            historyManager.fetchHistory()
        }
    }

    private fun onError() {
        Snackbar.make(view!!, R.string.error_network, LENGTH_SHORT).show()
    }

    override fun onRefundClicked(item: HistoryItem) {
        refundManager.startRefund(item)
        navigate(actionNavHistoryToRefundFragment())
    }

}

private class HistoryItemAdapter(private val listener: RefundClickListener) :
    Adapter<HistoryItemViewHolder>() {

    private val items = ArrayList<HistoryItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItemViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_history, parent, false)
        return HistoryItemViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: HistoryItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setData(items: List<HistoryItem>) {
        this.items.clear()
        this.items.addAll(items)
        this.notifyDataSetChanged()
    }

    private inner class HistoryItemViewHolder(private val v: View) : ViewHolder(v) {

        private val orderSummaryView: TextView = v.findViewById(R.id.orderSummaryView)
        private val orderAmountView: TextView = v.findViewById(R.id.orderAmountView)
        private val orderTimeView: TextView = v.findViewById(R.id.orderTimeView)
        private val orderIdView: TextView = v.findViewById(R.id.orderIdView)
        private val refundButton: ImageButton = v.findViewById(R.id.refundButton)

        fun bind(item: HistoryItem) {
            orderSummaryView.text = item.summary
            val amount = item.amount
            @SuppressLint("SetTextI18n")
            orderAmountView.text = "${amount.amount} ${amount.currency}"
            orderIdView.text = v.context.getString(R.string.history_ref_no, item.orderId)
            orderTimeView.text = item.time.toRelativeTime(v.context)
            refundButton.setOnClickListener { listener.onRefundClicked(item) }
        }

    }

}
