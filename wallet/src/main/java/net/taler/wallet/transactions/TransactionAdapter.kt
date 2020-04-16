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
import net.taler.wallet.transactions.TransactionAdapter.TransactionViewHolder


internal class TransactionAdapter(
    private val devMode: Boolean,
    private val listener: OnEventClickListener,
    private var transactions: Transactions = Transactions()
) : Adapter<TransactionViewHolder>() {

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
        holder.bind(transaction, tracker.isSelected(transaction.eventId))
    }

    fun update(updatedTransactions: Transactions) {
        this.transactions = updatedTransactions
        this.notifyDataSetChanged()
    }

    fun selectAll() = transactions.forEach {
        tracker.select(it.eventId)
    }

    internal open inner class TransactionViewHolder(private val v: View) : ViewHolder(v) {

        protected val context: Context = v.context

        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)
        private val amount: TextView = v.findViewById(R.id.amount)

        private val selectableBackground = v.background
        private val amountColor = amount.currentTextColor

        open fun bind(transaction: Transaction, selected: Boolean) {
            if (devMode || transaction.detailPageLayout != 0) {
                v.background = selectableBackground
                v.setOnClickListener { listener.onTransactionClicked(transaction) }
            } else {
                v.background = null
                v.setOnClickListener(null)
            }
            v.isActivated = selected
            icon.setImageResource(transaction.icon)

            title.text = if (transaction.title == null) {
                when (transaction) {
                    is RefreshTransaction -> getRefreshTitle(transaction)
                    is OrderAcceptedTransaction -> context.getString(R.string.transaction_order_accepted)
                    is OrderRefusedTransaction -> context.getString(R.string.transaction_order_refused)
                    is TipAcceptedTransaction -> context.getString(R.string.transaction_tip_accepted)
                    is TipDeclinedTransaction -> context.getString(R.string.transaction_tip_declined)
                    is ReserveBalanceUpdatedTransaction -> context.getString(R.string.transaction_reserve_balance_updated)
                    else -> transaction::class.java.simpleName
                }
            } else transaction.title

            time.text = transaction.timestamp.ms.toRelativeTime(context)
            bindAmount(transaction.displayAmount)
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

        private fun getRefreshTitle(transaction: RefreshTransaction): String {
            val res = when (transaction.refreshReason) {
                RefreshReason.MANUAL -> R.string.transaction_refresh_reason_manual
                RefreshReason.PAY -> R.string.transaction_refresh_reason_pay
                RefreshReason.REFUND -> R.string.transaction_refresh_reason_refund
                RefreshReason.ABORT_PAY -> R.string.transaction_refresh_reason_abort_pay
                RefreshReason.RECOUP -> R.string.transaction_refresh_reason_recoup
                RefreshReason.BACKUP_RESTORED -> R.string.transaction_refresh_reason_backup_restored
            }
            return context.getString(R.string.transaction_refresh) + " " + context.getString(res)
        }

    }

    internal inner class TransactionKeyProvider : ItemKeyProvider<String>(SCOPE_MAPPED) {
        override fun getKey(position: Int) = transactions[position].eventId
        override fun getPosition(key: String): Int {
            return transactions.indexOfFirst { it.eventId == key }
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
