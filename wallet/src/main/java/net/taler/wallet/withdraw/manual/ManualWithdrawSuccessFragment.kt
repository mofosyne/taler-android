/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.withdraw.manual

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.openUri
import net.taler.common.shareText
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.withdraw.TransferData
import net.taler.wallet.withdraw.WithdrawStatus

class ManualWithdrawSuccessFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var status: WithdrawStatus.ManualTransferRequired

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        status = withdrawManager.withdrawStatus.value as WithdrawStatus.ManualTransferRequired

        // Set action bar subtitle and unset on exit
        if (status.withdrawalTransfers.size > 1) {
            val activity = requireActivity() as AppCompatActivity

            activity.apply {
                supportActionBar?.subtitle = getString(R.string.withdraw_subtitle)
            }

            findNavController().addOnDestinationChangedListener { controller, destination, args ->
                if (destination.id != R.id.nav_exchange_manual_withdrawal_success) {
                    activity.apply {
                        supportActionBar?.subtitle = null
                    }
                }
            }
        }

        setContent {
            TalerSurface {
                ScreenTransfer(
                    status = status,
                    bankAppClick = { onBankAppClick(it) },
                    shareClick = { onShareClick(it) },
                )
            }
        }
    }

    private fun onBankAppClick(transfer: TransferData) {
        requireContext().openUri(
            uri = transfer.withdrawalAccount.paytoUri,
            title = requireContext().getString(R.string.share_payment)
        )
    }

    private fun onShareClick(transfer: TransferData) {
        requireContext().shareText(
            text = transfer.withdrawalAccount.paytoUri,
        )
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.withdraw_title)
    }
}
