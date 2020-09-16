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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.taler.common.toRelativeTime
import net.taler.merchantlib.OrderHistoryEntry
import net.taler.merchantpos.R
import net.taler.merchantpos.history.HistoryItemAdapter.HistoryItemViewHolder
import java.util.ArrayList


internal class HistoryItemAdapter(private val listener: RefundClickListener) :
    Adapter<HistoryItemViewHolder>() {

    private val items = ArrayList<OrderHistoryEntry>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryItemViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_history, parent, false)
        return HistoryItemViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: HistoryItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setData(items: List<OrderHistoryEntry>) {
        this.items.clear()
        this.items.addAll(items)
        this.notifyDataSetChanged()
    }

    internal inner class HistoryItemViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {

        private val orderSummaryView: TextView = v.findViewById(R.id.orderSummaryView)
        private val orderAmountView: TextView = v.findViewById(R.id.orderAmountView)
        private val orderTimeView: TextView = v.findViewById(R.id.orderTimeView)
        private val orderIdView: TextView = v.findViewById(R.id.orderIdView)
        private val refundButton: ImageButton = v.findViewById(R.id.refundButton)

        private val orderIdColor = orderIdView.currentTextColor

        fun bind(item: OrderHistoryEntry) {
            orderSummaryView.text = item.summary
            val amount = item.amount
            orderAmountView.text = amount.toString()
            orderTimeView.text = item.timestamp.ms.toRelativeTime(v.context)
            if (item.paid) {
                orderIdView.text = v.context.getString(R.string.history_ref_no, item.orderId)
                orderIdView.setTextColor(orderIdColor)
            } else {
                orderIdView.text = v.context.getString(R.string.history_unpaid)
                orderIdView.setTextColor(getColor(v.context, R.color.red))
            }
            if (item.refundable) {
                refundButton.visibility = View.VISIBLE
                refundButton.setOnClickListener { listener.onRefundClicked(item) }
            } else {
                refundButton.visibility = View.GONE
            }
        }

    }

}
