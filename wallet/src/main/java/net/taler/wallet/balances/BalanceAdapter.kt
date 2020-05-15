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

package net.taler.wallet.balances

import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.balances.BalanceAdapter.BalanceViewHolder

data class BalanceItem(val available: Amount, val pendingIncoming: Amount, val hasPending: Boolean)

class BalanceAdapter(private val listener: BalanceClickListener) : Adapter<BalanceViewHolder>() {

    private var items = emptyList<BalanceItem>()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BalanceViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_balance, parent, false)
        return BalanceViewHolder(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BalanceViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    fun setItems(items: List<BalanceItem>) {
        this.items = items
        this.notifyDataSetChanged()
    }

    inner class BalanceViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        private val currencyView: TextView = v.findViewById(R.id.balanceCurrencyView)
        private val amountView: TextView = v.findViewById(R.id.balanceAmountView)
        private val balanceInboundAmount: TextView = v.findViewById(R.id.balanceInboundAmount)
        private val balanceInboundLabel: TextView = v.findViewById(R.id.balanceInboundLabel)
        private val pendingView: TextView = v.findViewById(R.id.pendingView)

        fun bind(item: BalanceItem) {
            v.setOnClickListener { listener.onBalanceClick(item.available.currency) }
            currencyView.text = item.available.currency
            amountView.text = item.available.amountStr

            val amountIncoming = item.pendingIncoming
            if (amountIncoming.isZero()) {
                balanceInboundAmount.visibility = GONE
                balanceInboundLabel.visibility = GONE
            } else {
                balanceInboundAmount.visibility = VISIBLE
                balanceInboundLabel.visibility = VISIBLE
                balanceInboundAmount.text =
                    v.context.getString(R.string.amount_positive, amountIncoming)
            }
            pendingView.visibility = if (item.hasPending) VISIBLE else GONE
        }
    }

}
