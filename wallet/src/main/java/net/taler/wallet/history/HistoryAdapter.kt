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

import android.annotation.SuppressLint
import android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.Amount
import net.taler.common.toRelativeTime
import net.taler.wallet.BuildConfig
import net.taler.wallet.R
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

    internal abstract inner class HistoryEventViewHolder(protected val v: View) : ViewHolder(v) {

        private val icon: ImageView = v.findViewById(R.id.icon)
        protected val title: TextView = v.findViewById(R.id.title)
        private val time: TextView = v.findViewById(R.id.time)

        @CallSuper
        open fun bind(event: HistoryEvent) {
            if (BuildConfig.DEBUG) {  // doesn't produce recycling issues, no need to cover all cases
                v.setOnClickListener { listener.onEventClicked(event) }
            } else {
                v.background = null
            }
            icon.setImageResource(event.icon)
            if (event.title == 0) title.text = event::class.java.simpleName
            else title.setText(event.title)
            time.text = event.timestamp.ms.toRelativeTime(v.context)
        }

    }

    internal inner class GenericHistoryEventViewHolder(v: View) : HistoryEventViewHolder(v) {

        private val info: TextView = v.findViewById(R.id.info)

        override fun bind(event: HistoryEvent) {
            super.bind(event)
            info.text = when (event) {
                is ExchangeAddedEvent -> event.exchangeBaseUrl
                is ExchangeUpdatedEvent -> event.exchangeBaseUrl
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
        private val feeLabel: TextView = v.findViewById(R.id.feeLabel)
        private val fee: TextView = v.findViewById(R.id.fee)

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
            title.text = getHostname(event.exchangeBaseUrl)
            summary.setText(event.title)

            showAmounts(event.amountWithdrawnEffective, event.amountWithdrawnRaw)
        }

        private fun bind(event: HistoryRefundedEvent) {
            title.text = event.orderShortInfo.summary
            summary.setText(event.title)

            showAmounts(event.amountRefundedEffective, event.amountRefundedRaw)
        }

        private fun bind(event: HistoryTipAcceptedEvent) {
            title.setText(event.title)
            summary.text = null
            showAmounts(event.tipRaw, event.tipRaw)
        }

        private fun bind(event: HistoryTipDeclinedEvent) {
            title.setText(event.title)
            summary.text = null
            showAmounts(event.tipAmount, event.tipAmount)
            amountWithdrawn.paintFlags = amountWithdrawn.paintFlags or STRIKE_THRU_TEXT_FLAG
        }

        private fun showAmounts(effective: Amount, raw: Amount) {
            @SuppressLint("SetTextI18n")
            amountWithdrawn.text = "+$raw"
            val calculatedFee = raw - effective
            if (calculatedFee.isZero()) {
                fee.visibility = GONE
                feeLabel.visibility = GONE
            } else {
                @SuppressLint("SetTextI18n")
                fee.text = "-$calculatedFee"
                fee.visibility = VISIBLE
                feeLabel.visibility = VISIBLE
            }
            amountWithdrawn.paintFlags = fee.paintFlags
        }

        private fun getHostname(url: String): String {
            return url.toUri().host!!
        }

    }

    internal inner class HistoryPaymentViewHolder(v: View) : HistoryEventViewHolder(v) {

        private val summary: TextView = v.findViewById(R.id.summary)
        private val amountPaidWithFees: TextView = v.findViewById(R.id.amountPaidWithFees)

        override fun bind(event: HistoryEvent) {
            super.bind(event)
            summary.setText(event.title)
            when (event) {
                is HistoryPaymentSentEvent -> bind(event)
                is HistoryPaymentAbortedEvent -> bind(event)
                is HistoryRefreshedEvent -> bind(event)
            }
        }

        private fun bind(event: HistoryPaymentSentEvent) {
            title.text = event.orderShortInfo.summary
            @SuppressLint("SetTextI18n")
            amountPaidWithFees.text = "-${event.amountPaidWithFees}"
        }

        private fun bind(event: HistoryPaymentAbortedEvent) {
            title.text = event.orderShortInfo.summary
            @SuppressLint("SetTextI18n")
            amountPaidWithFees.text = "-${event.amountLost}"
        }

        private fun bind(event: HistoryRefreshedEvent) {
            title.text = ""
            val fee = event.amountRefreshedRaw - event.amountRefreshedEffective
            @SuppressLint("SetTextI18n")
            if (fee.isZero()) amountPaidWithFees.text = null
            else amountPaidWithFees.text = "-$fee"
        }

    }

}
