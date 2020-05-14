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

package net.taler.wallet.transactions

import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.exhaustive
import net.taler.common.toRelativeTime
import net.taler.wallet.R
import net.taler.wallet.history.AmountType
import net.taler.wallet.transactions.TransactionAdapter.TransactionViewHolder

internal class TransactionAdapter(
    private val listener: OnTransactionClickListener
) : Adapter<TransactionViewHolder>() {

    private var transactions: List<Transaction> = ArrayList()
    lateinit var tracker: SelectionTracker<String>
    val keyProvider = TransactionKeyProvider()

    init {
        setHasStableIds(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction, tracker.isSelected(transaction.transactionId))
    }

    fun update(updatedTransactions: List<Transaction>) {
        this.transactions = updatedTransactions
        this.notifyDataSetChanged()
    }

    fun selectAll() = transactions.forEach {
        tracker.select(it.transactionId)
    }

    internal open inner class TransactionViewHolder(private val v: View) : ViewHolder(v) {

        protected val context: Context = v.context

        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)
        private val amount: TextView = v.findViewById(R.id.amount)
        private val pendingView: TextView = v.findViewById(R.id.pendingView)

        private val selectableForeground = v.foreground
        private val amountColor = amount.currentTextColor
        private val red = context.getColor(R.color.red)
        private val green = context.getColor(R.color.green)

        open fun bind(transaction: Transaction, selected: Boolean) {
            if (transaction.detailPageLayout != 0) {
                v.foreground = selectableForeground
                v.setOnClickListener { listener.onTransactionClicked(transaction) }
            } else {
                v.foreground = null
                v.setOnClickListener(null)
            }
            v.isActivated = selected
            icon.setImageResource(transaction.icon)

            title.text = transaction.getTitle(context)
            time.text = transaction.timestamp.ms.toRelativeTime(context)
            bindAmount(transaction)
            pendingView.visibility = if (transaction.pending) VISIBLE else GONE
        }

        private fun bindAmount(transaction: Transaction) {
            val amountEffective = transaction.amountEffective
            if (amountEffective == null) {
                amount.visibility = GONE
            } else {
                amount.visibility = VISIBLE
                when (transaction.amountType) {
                    AmountType.Positive -> {
                        amount.text =
                            context.getString(R.string.amount_positive, amountEffective.amountStr)
                        amount.setTextColor(if (transaction.pending) amountColor else green)
                    }
                    AmountType.Negative -> {
                        amount.text =
                            context.getString(R.string.amount_negative, amountEffective.amountStr)
                        amount.setTextColor(if (transaction.pending) amountColor else red)
                    }
                    AmountType.Neutral -> {
                        amount.text = amountEffective.amountStr
                        amount.setTextColor(amountColor)
                    }
                }.exhaustive
            }
        }

    }

    internal inner class TransactionKeyProvider : ItemKeyProvider<String>(SCOPE_MAPPED) {
        override fun getKey(position: Int) = transactions[position].transactionId
        override fun getPosition(key: String): Int {
            return transactions.indexOfFirst { it.transactionId == key }
        }
    }

}

internal class TransactionLookup(
    private val list: RecyclerView,
    private val adapter: TransactionAdapter
) : ItemDetailsLookup<String>() {
    override fun getItemDetails(e: MotionEvent): ItemDetails<String>? {
        list.findChildViewUnder(e.x, e.y)?.let { view ->
            val holder = list.getChildViewHolder(view)
            val position = holder.adapterPosition
            return object : ItemDetails<String>() {
                override fun getPosition(): Int = position
                override fun getSelectionKey(): String = adapter.keyProvider.getKey(position)
            }
        }
        return null
    }
}
