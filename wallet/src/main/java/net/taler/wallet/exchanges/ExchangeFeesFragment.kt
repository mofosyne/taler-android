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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.Amount
import net.taler.common.toRelativeTime
import net.taler.common.toShortDate
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentExchangeFeesBinding
import net.taler.wallet.exchanges.CoinFeeAdapter.CoinFeeViewHolder
import net.taler.wallet.exchanges.WireFeeAdapter.WireFeeViewHolder
import net.taler.wallet.getAttrColor

class ExchangeFeesFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var ui: FragmentExchangeFeesBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentExchangeFeesBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fees = withdrawManager.exchangeFees ?: throw IllegalStateException()
        if (fees.withdrawFee.isZero()) {
            ui.withdrawFeeLabel.visibility = GONE
            ui.withdrawFeeView.visibility = GONE
        } else ui.withdrawFeeView.setAmount(fees.withdrawFee)
        if (fees.overhead.isZero()) {
            ui.overheadLabel.visibility = GONE
            ui.overheadView.visibility = GONE
        } else ui.overheadView.setAmount(fees.overhead)
        ui.expirationView.text = fees.earliestDepositExpiration.ms.toRelativeTime(requireContext())
        ui.coinFeesList.adapter = CoinFeeAdapter(fees.coinFees)
        ui.wireFeesList.adapter = WireFeeAdapter(fees.wireFees)
    }

    private fun TextView.setAmount(amount: Amount) {
        if (amount.isZero()) text = amount.toString()
        else {
            text = getString(R.string.amount_negative, amount)
            setText(requireContext().getAttrColor(R.attr.colorError))
        }
    }

}

private class CoinFeeAdapter(private val items: List<CoinFee>) : Adapter<CoinFeeViewHolder>() {
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoinFeeViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_coin_fee, parent, false)
        return CoinFeeViewHolder(v)
    }

    override fun onBindViewHolder(holder: CoinFeeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class CoinFeeViewHolder(private val v: View) : ViewHolder(v) {
        private val res = v.context.resources
        private val coinView: TextView = v.findViewById(R.id.coinView)
        private val withdrawFeeView: TextView = v.findViewById(R.id.withdrawFeeView)
        private val depositFeeView: TextView = v.findViewById(R.id.depositFeeView)
        private val refreshFeeView: TextView = v.findViewById(R.id.refreshFeeView)
        private val refundFeeView: TextView = v.findViewById(R.id.refundFeeView)
        fun bind(item: CoinFee) {
            coinView.text = res.getQuantityString(
                R.plurals.exchange_fee_coin,
                item.quantity,
                item.coin,
                item.quantity
            )
            withdrawFeeView.text =
                v.context.getString(R.string.exchange_fee_withdraw_fee, item.feeWithdraw)
            depositFeeView.text =
                v.context.getString(R.string.exchange_fee_deposit_fee, item.feeDeposit)
            refreshFeeView.text =
                v.context.getString(R.string.exchange_fee_refresh_fee, item.feeRefresh)
            refundFeeView.text =
                v.context.getString(R.string.exchange_fee_refund_fee, item.feeRefresh)
        }
    }
}

private class WireFeeAdapter(private val items: List<WireFee>) : Adapter<WireFeeViewHolder>() {
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WireFeeViewHolder {
        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_wire_fee, parent, false)
        return WireFeeViewHolder(v)
    }

    override fun onBindViewHolder(holder: WireFeeViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class WireFeeViewHolder(private val v: View) : ViewHolder(v) {
        private val validityView: TextView = v.findViewById(R.id.validityView)
        private val wireFeeView: TextView = v.findViewById(R.id.wireFeeView)
        private val closingFeeView: TextView = v.findViewById(R.id.closingFeeView)
        fun bind(item: WireFee) {
            validityView.text = v.context.getString(
                R.string.exchange_fee_wire_fee_timespan,
                item.start.ms.toShortDate(v.context),
                item.end.ms.toShortDate(v.context)
            )
            wireFeeView.text =
                v.context.getString(R.string.exchange_fee_wire_fee_wire_fee, item.wireFee)
            closingFeeView.text =
                v.context.getString(R.string.exchange_fee_wire_fee_closing_fee, item.closingFee)
        }
    }
}
