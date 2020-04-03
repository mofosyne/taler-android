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
import net.taler.wallet.history.HistoryAdapter.HistoryEventViewHolder


internal class HistoryAdapter(
    private val listener: OnEventClickListener,
    private var history: History = History()
) : Adapter<HistoryEventViewHolder>() {

    init {
        setHasStableIds(false)
    }

    override fun getItemViewType(position: Int): Int = history[position].layout

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.history_receive -> HistoryReceiveViewHolder(view)
            R.layout.history_payment -> HistoryPaymentViewHolder(view)
            else -> GenericHistoryEventViewHolder(view)
        }
    }

    override fun getItemCount(): Int = history.size

    override fun onBindViewHolder(holder: HistoryEventViewHolder, position: Int) {
        val event = history[position]
        holder.bind(event)
    }

    fun update(updatedHistory: History) {
        this.history = updatedHistory
        this.notifyDataSetChanged()
    }

    internal abstract inner class HistoryEventViewHolder(private val v: View) : ViewHolder(v) {

        protected val context: Context = v.context
        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)

        @CallSuper
        open fun bind(event: HistoryEvent) {
            v.setOnClickListener { listener.onEventClicked(event) }
            icon.setImageResource(event.icon)
            if (event.title == 0) title.text = event::class.java.simpleName
            else title.setText(event.title)
            time.text = event.timestamp.ms.toRelativeTime(context)
        }

    }

    internal inner class GenericHistoryEventViewHolder(v: View) : HistoryEventViewHolder(v) {

        private val info: TextView = v.findViewById(R.id.info)

        override fun bind(event: HistoryEvent) {
            super.bind(event)
            info.text = when (event) {
                is ExchangeAddedEvent -> cleanExchange(event.exchangeBaseUrl)
                is ExchangeUpdatedEvent -> cleanExchange(event.exchangeBaseUrl)
                is ReserveBalanceUpdatedEvent -> event.amountReserveBalance.toString()
                is HistoryPaymentSentEvent -> event.orderShortInfo.summary
                is HistoryOrderAcceptedEvent -> event.orderShortInfo.summary
                is HistoryOrderRefusedEvent -> event.orderShortInfo.summary
                is HistoryOrderRedirectedEvent -> event.newOrderShortInfo.summary
                else -> ""
            }
        }

    }

    internal inner class HistoryReceiveViewHolder(v: View) : HistoryEventViewHolder(v) {

        private val summary: TextView = v.findViewById(R.id.summary)
        private val amountWithdrawn: TextView = v.findViewById(R.id.amountWithdrawn)
        private val paintFlags = amountWithdrawn.paintFlags

        override fun bind(event: HistoryEvent) {
            super.bind(event)
            when (event) {
                is HistoryWithdrawnEvent -> bind(event)
                is HistoryRefundedEvent -> bind(event)
                is HistoryTipAcceptedEvent -> bind(event)
                is HistoryTipDeclinedEvent -> bind(event)
            }
        }

        private fun bind(event: HistoryWithdrawnEvent) {
            summary.text = cleanExchange(event.exchangeBaseUrl)
            amountWithdrawn.text =
                context.getString(R.string.amount_positive, event.amountWithdrawnEffective)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(event: HistoryRefundedEvent) {
            summary.text = event.orderShortInfo.summary
            amountWithdrawn.text =
                context.getString(R.string.amount_positive, event.amountRefundedEffective)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(event: HistoryTipAcceptedEvent) {
            summary.text = null
            amountWithdrawn.text = context.getString(R.string.amount_positive, event.tipRaw)
            amountWithdrawn.paintFlags = paintFlags
        }

        private fun bind(event: HistoryTipDeclinedEvent) {
            summary.text = null
            amountWithdrawn.text = context.getString(R.string.amount_positive, event.tipAmount)
            amountWithdrawn.paintFlags = amountWithdrawn.paintFlags or STRIKE_THRU_TEXT_FLAG
        }

    }

    internal inner class HistoryPaymentViewHolder(v: View) : HistoryEventViewHolder(v) {

        private val summary: TextView = v.findViewById(R.id.summary)
        private val amountPaidWithFees: TextView = v.findViewById(R.id.amountPaidWithFees)

        override fun bind(event: HistoryEvent) {
            super.bind(event)
            when (event) {
                is HistoryPaymentSentEvent -> bind(event)
                is HistoryPaymentAbortedEvent -> bind(event)
                is HistoryRefreshedEvent -> bind(event)
            }
        }

        private fun bind(event: HistoryPaymentSentEvent) {
            summary.text = event.orderShortInfo.summary
            amountPaidWithFees.text =
                context.getString(R.string.amount_negative, event.amountPaidWithFees)
        }

        private fun bind(event: HistoryPaymentAbortedEvent) {
            summary.text = event.orderShortInfo.summary
            amountPaidWithFees.text = context.getString(R.string.amount_negative, event.amountLost)
        }

        private fun bind(event: HistoryRefreshedEvent) {
            val res = when (event.refreshReason) {
                RefreshReason.MANUAL -> R.string.history_event_refresh_reason_manual
                RefreshReason.PAY -> R.string.history_event_refresh_reason_pay
                RefreshReason.REFUND -> R.string.history_event_refresh_reason_refund
                RefreshReason.ABORT_PAY -> R.string.history_event_refresh_reason_abort_pay
                RefreshReason.RECOUP -> R.string.history_event_refresh_reason_recoup
                RefreshReason.BACKUP_RESTORED -> R.string.history_event_refresh_reason_backup_restored
            }
            summary.text = context.getString(res)
            val fee = event.amountRefreshedRaw - event.amountRefreshedEffective
            if (fee.isZero()) amountPaidWithFees.text = null
            else amountPaidWithFees.text = context.getString(R.string.amount_negative, fee)
        }

    }

}
