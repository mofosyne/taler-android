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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.startActivitySafe
import net.taler.common.toAbsoluteTime
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.databinding.FragmentTransactionWithdrawalBinding
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer
import net.taler.wallet.transactions.WithdrawalDetails.TalerBankIntegrationApi
import net.taler.wallet.withdraw.createManualTransferRequired

class TransactionWithdrawalFragment : TransactionDetailFragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }
    private lateinit var ui: FragmentTransactionWithdrawalBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentTransactionWithdrawalBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val t = transaction as TransactionWithdrawal
        ui.timeView.text = t.timestamp.ms.toAbsoluteTime(requireContext())

        ui.effectiveAmountLabel.text = getString(R.string.withdraw_total)
        ui.effectiveAmountView.text = t.amountEffective.toString()
        setupConfirmWithdrawalButton(t)
        setupActionButton(t)
        ui.chosenAmountLabel.text = getString(R.string.amount_chosen)
        ui.chosenAmountView.text =
            getString(R.string.amount_positive, t.amountRaw.toString())
        val fee = t.amountRaw - t.amountEffective
        ui.feeView.text = getString(R.string.amount_negative, fee.toString())
        ui.exchangeView.text = cleanExchange(t.exchangeBaseUrl)
        if (t.pending) {
            ui.deleteButton.setIconResource(R.drawable.ic_cancel)
            ui.deleteButton.setText(R.string.cancel)
        }
        ui.deleteButton.setOnClickListener {
            onDeleteButtonClicked(t)
        }
    }

    override val deleteDialogTitle: Int
        get() = if (transaction?.pending == true) R.string.cancel else super.deleteDialogTitle
    override val deleteDialogMessage: Int
        get() = if (transaction?.pending == true) R.string.transactions_cancel_dialog_message
        else super.deleteDialogMessage
    override val deleteDialogButton: Int
        get() = if (transaction?.pending == true) R.string.ok else super.deleteDialogButton

    private fun setupConfirmWithdrawalButton(t: TransactionWithdrawal) {
        if (t.pending && !t.confirmed) {
            if (t.withdrawalDetails is TalerBankIntegrationApi &&
                t.withdrawalDetails.bankConfirmationUrl != null
            ) {
                val i = Intent(ACTION_VIEW).apply {
                    data = Uri.parse(t.withdrawalDetails.bankConfirmationUrl)
                }
                ui.confirmWithdrawalButton.setOnClickListener { startActivitySafe(i) }
            } else if (t.withdrawalDetails is ManualTransfer) {
                ui.confirmWithdrawalButton.setText(R.string.withdraw_manual_ready_details_intro)
                ui.confirmWithdrawalButton.setOnClickListener {
                    val status = createManualTransferRequired(
                        amount = t.amountRaw,
                        exchangeBaseUrl = t.exchangeBaseUrl,
                        // TODO what if there's more than one or no URI?
                        uriStr = t.withdrawalDetails.exchangePaytoUris[0],
                        transactionId = t.transactionId,
                    )
                    withdrawManager.viewManualWithdrawal(status)
                    findNavController().navigate(
                        R.id.action_nav_transactions_detail_withdrawal_to_nav_exchange_manual_withdrawal_success)
                }
            } else ui.confirmWithdrawalButton.visibility = View.GONE
        } else ui.confirmWithdrawalButton.visibility = View.GONE
    }

    private fun setupActionButton(t: TransactionWithdrawal) {
        ui.actionButton.visibility = t.error?.let { error ->
            when (error.code) {
                7025 -> { // KYC
                    ui.actionButton.setText(R.string.transaction_action_kyc)
                    val i = Intent(ACTION_VIEW).apply {
                        data = Uri.parse(error.kycUrl)
                    }
                    ui.actionButton.setOnClickListener { startActivitySafe(i) }
                    View.VISIBLE
                }
                else -> View.GONE
            }
        } ?: View.GONE
    }
}
