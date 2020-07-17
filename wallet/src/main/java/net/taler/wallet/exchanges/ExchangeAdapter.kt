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
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.exchanges.ExchangeAdapter.ExchangeItemViewHolder

data class ExchangeItem(
    val exchangeBaseUrl: String,
    val currency: String,
    val paytoUris: List<String>
)

internal class ExchangeAdapter : RecyclerView.Adapter<ExchangeItemViewHolder>() {

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
        fun bind(item: ExchangeItem) {
            urlView.text = cleanExchange(item.exchangeBaseUrl)
            currencyView.text = context.getString(R.string.exchange_list_currency, item.currency)
        }
    }

}
