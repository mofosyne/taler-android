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

package net.taler.wallet.exchanges

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.taler.wallet.R
import net.taler.wallet.exchanges.ExchangeAdapter.ExchangeItemViewHolder

interface ExchangeClickListener {
    fun onExchangeSelected(item: ExchangeItem)
    fun onManualWithdraw(item: ExchangeItem)
    fun onPeerReceive(item: ExchangeItem)
    fun onExchangeDelete(item: ExchangeItem)
}

internal class ExchangeAdapter(
    private val selectOnly: Boolean,
    private val listener: ExchangeClickListener,
) : Adapter<ExchangeItemViewHolder>() {

    private val items = ArrayList<ExchangeItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExchangeItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_exchange, parent, false)
        return ExchangeItemViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ExchangeItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun update(newItems: List<ExchangeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    internal inner class ExchangeItemViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val context = v.context
        private val urlView: TextView = v.findViewById(R.id.urlView)
        private val currencyView: TextView = v.findViewById(R.id.currencyView)
        private val overflowIcon: ImageButton = v.findViewById(R.id.overflowIcon)

        fun bind(item: ExchangeItem) {
            urlView.text = item.name
            // If currency is null, it's because we have no data from the exchange...
            currencyView.text = if (item.currency == null) {
                context.getString(R.string.exchange_not_contacted)
            } else {
                context.getString(R.string.exchange_list_currency, item.currency)
            }
            if (selectOnly) {
                itemView.setOnClickListener { listener.onExchangeSelected(item) }
                overflowIcon.visibility = GONE
            } else {
                itemView.setOnClickListener(null)
                itemView.isClickable = false
                // ...thus, we should prevent the user from interacting with it.
                overflowIcon.visibility = if (item.currency != null) VISIBLE else GONE
            }
            overflowIcon.setOnClickListener { openMenu(overflowIcon, item) }
        }

        private fun openMenu(anchor: View, item: ExchangeItem) = PopupMenu(context, anchor).apply {
            inflate(R.menu.exchange)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_manual_withdrawal -> {
                        listener.onManualWithdraw(item)
                        true
                    }
                    R.id.action_receive_peer -> {
                        listener.onPeerReceive(item)
                        true
                    }
                    R.id.action_delete -> {
                        listener.onExchangeDelete(item)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

}
