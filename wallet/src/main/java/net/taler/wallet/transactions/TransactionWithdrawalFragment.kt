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
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.launchInAppBrowser
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer
import net.taler.wallet.transactions.WithdrawalDetails.TalerBankIntegrationApi
import net.taler.wallet.withdraw.TransactionWithdrawalComposable
import net.taler.wallet.withdraw.createManualTransferRequired

class TransactionWithdrawalFragment : TransactionDetailFragment(), ActionListener {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            TalerSurface {
                val t = transactionManager.selectedTransaction.observeAsState().value
                if (t is TransactionWithdrawal) TransactionWithdrawalComposable(
                    t = t,
                    devMode = devMode,
                    actionListener = this@TransactionWithdrawalFragment,
                ) {
                    onTransitionButtonClicked(t, it)
                }
            }
        }
    }

    override fun onActionButtonClicked(tx: Transaction, type: ActionListener.Type) {
        when (type) {
            ActionListener.Type.COMPLETE_KYC -> {
                if (tx !is TransactionWithdrawal) return
                tx.kycUrl?.let {
                    launchInAppBrowser(requireContext(), it)
                }
            }

            ActionListener.Type.CONFIRM_WITH_BANK -> {
                if (tx !is TransactionWithdrawal) return
                if (tx.withdrawalDetails !is TalerBankIntegrationApi) return
                tx.withdrawalDetails.bankConfirmationUrl?.let { url ->
                    launchInAppBrowser(requireContext(), url)
                }
            }

            ActionListener.Type.CONFIRM_MANUAL -> {
                if (tx !is TransactionWithdrawal) return
                if (tx.withdrawalDetails !is ManualTransfer) return
                if (tx.withdrawalDetails.exchangeCreditAccountDetails.isNullOrEmpty()) return
                val status = createManualTransferRequired(
                    transactionId = tx.transactionId,
                    exchangeBaseUrl = tx.exchangeBaseUrl,
                    amountRaw = tx.amountRaw,
                    amountEffective = tx.amountEffective,
                    withdrawalAccountList = tx.withdrawalDetails.exchangeCreditAccountDetails,
                )
                withdrawManager.viewManualWithdrawal(status)
                findNavController().navigate(
                    R.id.action_nav_transactions_detail_withdrawal_to_nav_exchange_manual_withdrawal_success,
                )
            }
        }
    }
}
