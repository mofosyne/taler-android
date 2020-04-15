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
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.toRelativeTime
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.transactions.TransactionAdapter.TransactionViewHolder


internal class TransactionAdapter(
    private val devMode: Boolean,
    private val listener: OnEventClickListener,
    private var transactions: Transactions = Transactions()
) : Adapter<TransactionViewHolder>() {

    init {
        setHasStableIds(false)
    }

    override fun getItemViewType(position: Int): Int = transactions[position].layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.transaction_in -> TransactionInViewHolder(view)
            R.layout.transaction_out -> TransactionOutViewHolder(view)
            else -> GenericTransactionViewHolder(view)
        }
    }

    override fun getItemCount(): Int = transactions.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val event = transactions[position]
        holder.bind(event)
    }

    fun update(updatedTransactions: Transactions) {
        this.transactions = updatedTransactions
        this.notifyDataSetChanged()
    }

    internal abstract inner class TransactionViewHolder(private val v: View) : ViewHolder(v) {

        protected val context: Context = v.context
        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)
        private val selectableBackground = v.background

        @CallSuper
        open fun bind(event: Transaction) {
            if (devMode || event.detailPageLayout != 0) {
                v.background = selectableBackground
                v.setOnClickListener { listener.onEventClicked(event) }
            } else {
                v.background = null
                v.setOnClickListener(null)
            }
            icon.setImageResource(event.icon)
            if (event.title == 0) title.text = event::class.java.simpleName
            else title.setText(event.title)
            time.text = event.timestamp.ms.toRelativeTime(context)
        }

    }

    internal inner class GenericTransactionViewHolder(v: View) : TransactionViewHolder(v) {

        private val info: TextView = v.findViewById(R.id.info)

        override fun bind(transaction: Transaction) {
            super.bind(transaction)
            info.text = when (transaction) {
                is ExchangeAddedEvent -> cleanExchange(transaction.exchangeBaseUrl)
                is ExchangeUpdatedEvent -> cleanExchange(transaction.exchangeBaseUrl)
                is ReserveBalanceUpdatedTransaction -> transaction.reserveBalance.toString()
                is PaymentTransaction -> transaction.orderShortInfo.summary
                is OrderAcceptedTransaction -> transaction.orderShortInfo.summary
                is OrderRefusedTransaction -> transaction.orderShortInfo.summary
                is OrderRedirectedTransaction -> transaction.newOrderShortInfo.summary
                else -> ""
            }
        }

    }

    internal inner class TransactionInViewHolder(v: View) : TransactionViewHolder(v) {

        private val summary: TextView = v.findViewById(R.id.summary)
        private val amountWithdrawn: TextView = v.findViewById(R.id.amountWithdrawn)
        private val paintFlags = amountWithdrawn.paintFlags

        override fun bind(event: Transaction) {
            super.bind(event)
            when (event) {
                is WithdrawTransaction -> bind(event)
                is RefundTransaction -> bind(event)
                is TipAcceptedTransaction -> bind(event)
                is TipDeclinedTransaction -> bind(event)
            }
        }

        private fun bind(event: WithdrawTransaction) {
            summary.text = cleanExchange(event.exchangeBaseUrl)
            amountWithdrawn.text =
                context.getString(R.string.amount_positive, event.amountWithdrawnEffective)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(event: RefundTransaction) {
            summary.text = event.orderShortInfo.summary
            amountWithdrawn.text =
                context.getString(R.string.amount_positive, event.amountRefundedEffective)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(transaction: TipAcceptedTransaction) {
            summary.text = null
            amountWithdrawn.text = context.getString(R.string.amount_positive, transaction.tipRaw)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(transaction: TipDeclinedTransaction) {
            summary.text = null
            amountWithdrawn.text = context.getString(R.string.amount_positive, transaction.tipAmount)
            amountWithdrawn.paintFlags = amountWithdrawn.paintFlags or STRIKE_THRU_TEXT_FLAG
        }

    }

    internal inner class TransactionOutViewHolder(v: View) : TransactionViewHolder(v) {

        private val summary: TextView = v.findViewById(R.id.summary)
        private val amountPaidWithFees: TextView = v.findViewById(R.id.amountPaidWithFees)

        override fun bind(event: Transaction) {
            super.bind(event)
            when (event) {
                is PaymentTransaction -> bind(event)
                is PaymentAbortedTransaction -> bind(event)
                is RefreshTransaction -> bind(event)
            }
        }

        private fun bind(event: PaymentTransaction) {
            summary.text = event.orderShortInfo.summary
            amountPaidWithFees.text =
                context.getString(R.string.amount_negative, event.amountPaidWithFees)
        }

        private fun bind(transaction: PaymentAbortedTransaction) {
            summary.text = transaction.orderShortInfo.summary
            amountPaidWithFees.text = context.getString(R.string.amount_negative, transaction.amountLost)
        }

        private fun bind(event: RefreshTransaction) {
            val res = when (event.refreshReason) {
                RefreshReason.MANUAL -> R.string.transaction_refresh_reason_manual
                RefreshReason.PAY -> R.string.transaction_refresh_reason_pay
                RefreshReason.REFUND -> R.string.transaction_refresh_reason_refund
                RefreshReason.ABORT_PAY -> R.string.transaction_refresh_reason_abort_pay
                RefreshReason.RECOUP -> R.string.transaction_refresh_reason_recoup
                RefreshReason.BACKUP_RESTORED -> R.string.transaction_refresh_reason_backup_restored
            }
            summary.text = context.getString(res)
            val fee = event.amountRefreshedRaw - event.amountRefreshedEffective
            if (fee.isZero()) amountPaidWithFees.text = null
            else amountPaidWithFees.text = context.getString(R.string.amount_negative, fee)
        }

    }

}
