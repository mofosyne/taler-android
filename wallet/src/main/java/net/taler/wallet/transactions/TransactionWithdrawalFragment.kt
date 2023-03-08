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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.toAbsoluteTime
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.databinding.FragmentTransactionWithdrawalBinding
import net.taler.wallet.launchInAppBrowser
import net.taler.wallet.transactions.ActionListener.Type.COMPLETE_KYC
import net.taler.wallet.transactions.ActionListener.Type.CONFIRM_MANUAL
import net.taler.wallet.transactions.ActionListener.Type.CONFIRM_WITH_BANK
import net.taler.wallet.transactions.ExtendedStatus.Pending
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer
import net.taler.wallet.transactions.WithdrawalDetails.TalerBankIntegrationApi
import net.taler.wallet.withdraw.createManualTransferRequired

class TransactionWithdrawalFragment : TransactionDetailFragment(), ActionListener {

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
        transactionManager.selectedTransaction.observe(viewLifecycleOwner) { t ->
            if (t !is TransactionWithdrawal) return@observe
            ui.timeView.text = t.timestamp.ms.toAbsoluteTime(requireContext())

            ui.effectiveAmountLabel.text = getString(R.string.withdraw_total)
            ui.effectiveAmountView.text = t.amountEffective.toString()
            setupActionButton(t)
            ui.chosenAmountLabel.text = getString(R.string.amount_chosen)
            ui.chosenAmountView.text =
                getString(R.string.amount_positive, t.amountRaw.toString())
            val fee = t.amountRaw - t.amountEffective
            ui.feeView.text = getString(R.string.amount_negative, fee.toString())
            ui.exchangeView.text = cleanExchange(t.exchangeBaseUrl)
            if (t.extendedStatus == Pending) {
                ui.deleteButton.setIconResource(R.drawable.ic_cancel)
                ui.deleteButton.setText(R.string.cancel)
            }
            ui.deleteButton.setOnClickListener {
                onDeleteButtonClicked(t)
            }
            if (devMode.value == true && t.error != null) {
                ui.showErrorButton.visibility = VISIBLE
                ui.showErrorButton.setOnClickListener {
                    onShowErrorButtonClicked(t)
                }
            }
        }
    }

    private val isPending get() = transactionManager.selectedTransaction.value?.extendedStatus == Pending

    override val deleteDialogTitle: Int
        get() = if (isPending) R.string.cancel else super.deleteDialogTitle
    override val deleteDialogMessage: Int
        get() = if (isPending) R.string.transactions_cancel_dialog_message
        else super.deleteDialogMessage
    override val deleteDialogButton: Int
        get() = if (isPending) R.string.ok else super.deleteDialogButton

    private fun setupActionButton(t: TransactionWithdrawal) {
        ui.actionButton.setContent {
            TalerSurface {
                ActionButton(tx = t, listener = this)
            }
        }
    }

    override fun onActionButtonClicked(tx: Transaction, type: ActionListener.Type) {
        when (type) {
            COMPLETE_KYC -> {
                tx.error?.getStringExtra("kycUrl")?.let {  kycUrl ->
                    launchInAppBrowser(requireContext(), kycUrl)
                }
            }
            CONFIRM_WITH_BANK -> {
                if (tx !is TransactionWithdrawal) return
                if (tx.withdrawalDetails !is TalerBankIntegrationApi) return
                tx.withdrawalDetails.bankConfirmationUrl?.let { url ->
                    launchInAppBrowser(requireContext(), url)
                }
            }
            CONFIRM_MANUAL -> {
                if (tx !is TransactionWithdrawal) return
                if (tx.withdrawalDetails !is ManualTransfer) return
                val status = createManualTransferRequired(
                    amount = tx.amountRaw,
                    exchangeBaseUrl = tx.exchangeBaseUrl,
                    // TODO what if there's more than one or no URI?
                    uriStr = tx.withdrawalDetails.exchangePaytoUris[0],
                    transactionId = tx.transactionId,
                )
                withdrawManager.viewManualWithdrawal(status)
                findNavController().navigate(
                    R.id.action_nav_transactions_detail_withdrawal_to_nav_exchange_manual_withdrawal_success
                )
            }
        }
    }
}
