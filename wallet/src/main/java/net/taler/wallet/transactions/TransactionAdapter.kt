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
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.material.button.MaterialButton
import net.taler.common.exhaustive
import net.taler.common.toRelativeTime
import net.taler.wallet.R
import net.taler.wallet.handleKyc
import net.taler.wallet.transactions.ExtendedStatus.Pending
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

    internal inner class TransactionViewHolder(private val v: View) : ViewHolder(v) {
        private val context: Context = v.context

        private val root: ViewGroup = v.findViewById(R.id.root)
        private val icon: ImageView = v.findViewById(R.id.icon)
        private val title: TextView = v.findViewById(R.id.title)
        private val extraInfoView: TextView = v.findViewById(R.id.extraInfoView)
        private val actionButton: MaterialButton = v.findViewById(R.id.actionButton)
        private val time: TextView = v.findViewById(R.id.time)
        private val amount: TextView = v.findViewById(R.id.amount)
        private val pendingView: TextView = v.findViewById(R.id.pendingView)

        private val amountColor = amount.currentTextColor
        private val red = getColor(context, R.color.red)
        private val green = getColor(context, R.color.green)

        fun bind(transaction: Transaction, selected: Boolean) {
            v.setOnClickListener { listener.onTransactionClicked(transaction) }
            if (transaction.error == null) {
                icon.setImageResource(transaction.icon)
            } else {
                icon.setImageResource(R.drawable.ic_error)
            }
            title.text = transaction.getTitle(context)
            bindExtraInfo(transaction)
            bindActionButton(transaction)
            time.text = transaction.timestamp.ms.toRelativeTime(context)
            bindAmount(transaction)
            pendingView.visibility = if (transaction.extendedStatus == Pending) VISIBLE else GONE
            val bgColor = getColor(context,
                if (selected) R.color.selectedBackground
                else android.R.color.transparent)
            root.setBackgroundColor(bgColor)
        }

        private fun bindExtraInfo(transaction: Transaction) {
            if (transaction.error != null) {
                extraInfoView.text =
                    context.getString(R.string.payment_error, transaction.error!!.userFacingMsg)
                extraInfoView.setTextColor(red)
                extraInfoView.visibility = VISIBLE
            } else if (transaction is TransactionWithdrawal && !transaction.confirmed) {
                extraInfoView.setText(R.string.withdraw_waiting_confirm)
                extraInfoView.setTextColor(amountColor)
                extraInfoView.visibility = VISIBLE
            } else if (transaction is TransactionPayment && transaction.status != PaymentStatus.Paid && transaction.status != PaymentStatus.Accepted) {
                extraInfoView.setText(if (transaction.status == PaymentStatus.Aborted) R.string.payment_aborted else R.string.payment_failed)
                extraInfoView.setTextColor(amountColor)
                extraInfoView.visibility = VISIBLE
            } else {
                extraInfoView.visibility = GONE
            }
        }

        private fun bindActionButton(t: Transaction) {
            actionButton.setOnClickListener { listener.onActionButtonClicked(t) }
            if (t.error != null) {
                actionButton.visibility = t.handleKyc({ GONE }) {
                    actionButton.setText(R.string.transaction_action_kyc)
                    VISIBLE
                }
            } else if (t is TransactionWithdrawal && !t.confirmed) {
                actionButton.setIconResource(R.drawable.ic_account_balance)
                actionButton.visibility =
                    if (t.withdrawalDetails is WithdrawalDetails.TalerBankIntegrationApi &&
                        t.withdrawalDetails.bankConfirmationUrl != null) {
                        actionButton.setText(R.string.withdraw_button_confirm_bank)
                        VISIBLE
                    } else GONE
            }
        }

        private fun bindAmount(transaction: Transaction) {
            val amountStr = transaction.amountEffective.amountStr
            when (transaction.amountType) {
                AmountType.Positive -> {
                    amount.text = context.getString(R.string.amount_positive, amountStr)
                    amount.setTextColor(if (transaction.extendedStatus == Pending) amountColor else green)
                }
                AmountType.Negative -> {
                    amount.text = context.getString(R.string.amount_negative, amountStr)
                    amount.setTextColor(if (transaction.extendedStatus == Pending) amountColor else red)
                }
                AmountType.Neutral -> {
                    amount.text = amountStr
                    amount.setTextColor(amountColor)
                }
            }.exhaustive
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
            val position = holder.bindingAdapterPosition
            return object : ItemDetails<String>() {
                override fun getPosition(): Int = position
                override fun getSelectionKey(): String = adapter.keyProvider.getKey(position)
            }
        }
        return null
    }
}
