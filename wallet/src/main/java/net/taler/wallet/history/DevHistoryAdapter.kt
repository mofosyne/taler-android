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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.exhaustive
import net.taler.common.toRelativeTime
import net.taler.wallet.R
import net.taler.wallet.history.DevHistoryAdapter.HistoryViewHolder
import net.taler.wallet.transactions.AmountType

internal class DevHistoryAdapter(
    private val listener: OnEventClickListener
) : Adapter<HistoryViewHolder>() {

    private var history: List<HistoryEvent> = ArrayList()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun getItemCount(): Int = history.size

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = history[position]
        holder.bind(transaction)
    }

    fun update(updatedHistory: List<HistoryEvent>) {
        this.history = updatedHistory
        this.notifyDataSetChanged()
    }

    internal open inner class HistoryViewHolder(private val v: View) : ViewHolder(v) {

        protected val context: Context = v.context

        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)
        private val amount: TextView = v.findViewById(R.id.amount)

        private val amountColor = amount.currentTextColor

        open fun bind(historyEvent: HistoryEvent) {
            v.setOnClickListener { listener.onTransactionClicked(historyEvent) }
            icon.setImageResource(historyEvent.icon)
            title.text = historyEvent.title
            time.text = historyEvent.timestamp.ms.toRelativeTime(context)
            bindAmount(historyEvent.displayAmount)
        }

        private fun bindAmount(displayAmount: DisplayAmount?) {
            if (displayAmount == null) {
                amount.visibility = GONE
            } else {
                amount.visibility = VISIBLE
                when (displayAmount.type) {
                    AmountType.Positive -> {
                        amount.text = context.getString(
                            R.string.amount_positive, displayAmount.amount.amountStr
                        )
                        amount.setTextColor(context.getColor(R.color.green))
                    }
                    AmountType.Negative -> {
                        amount.text = context.getString(
                            R.string.amount_negative, displayAmount.amount.amountStr
                        )
                        amount.setTextColor(context.getColor(R.color.red))
                    }
                    AmountType.Neutral -> {
                        amount.text = displayAmount.amount.amountStr
                        amount.setTextColor(amountColor)
                    }
                }.exhaustive
            }
        }

    }

}