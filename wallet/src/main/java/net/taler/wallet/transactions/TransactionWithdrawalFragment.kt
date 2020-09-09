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

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.taler.common.startActivitySafe
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.databinding.FragmentTransactionWithdrawalBinding

class TransactionWithdrawalFragment : TransactionDetailFragment() {

    private lateinit var ui: FragmentTransactionWithdrawalBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentTransactionWithdrawalBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val t = transaction as TransactionWithdrawal
        ui.timeView.text = t.timestamp.ms.toAbsoluteTime(requireContext())

        ui.effectiveAmountLabel.text = getString(R.string.withdraw_total)
        ui.effectiveAmountView.text = t.amountEffective.toString()
        if (t.pending && t.withdrawalDetails is WithdrawalDetails.TalerBankIntegrationApi &&
            !t.confirmed && t.withdrawalDetails.bankConfirmationUrl != null
        ) {
            val i = Intent(ACTION_VIEW).apply {
                data = Uri.parse(t.withdrawalDetails.bankConfirmationUrl)
            }
            ui.confirmWithdrawalButton.setOnClickListener { startActivitySafe(i) }
        } else ui.confirmWithdrawalButton.visibility = View.GONE
        ui.chosenAmountLabel.text = getString(R.string.amount_chosen)
        ui.chosenAmountView.text =
            getString(R.string.amount_positive, t.amountRaw.toString())
        val fee = t.amountRaw - t.amountEffective
        ui.feeView.text = getString(R.string.amount_negative, fee.toString())
        ui.exchangeView.text = cleanExchange(t.exchangeBaseUrl)
    }

}
