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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.startActivitySafe
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.withdraw.WithdrawStatus

class ManualWithdrawSuccessFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }
    private val withdrawManager by lazy { model.withdrawManager }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        val status = withdrawManager.withdrawStatus.value as WithdrawStatus.ManualTransferRequired
        val intent = Intent().apply {
            data = status.uri
        }
        // TODO test if this works with an actual payto:// handling app
        val componentName = intent.resolveActivity(requireContext().packageManager)
        val onBankAppClick = if (componentName == null) null else {
            { startActivitySafe(intent) }
        }
        val tid = status.transactionId
        val onCancelClick = if (tid == null) null else {
            {
                transactionManager.deleteTransaction(tid)
                findNavController().navigate(R.id.action_nav_exchange_manual_withdrawal_success_to_nav_main)
            }
        }
        setContent {
            MdcTheme {
                Surface {
                    when (status) {
                        is WithdrawStatus.ManualTransferRequiredBitcoin -> {
                            ScreenBitcoin(status, onBankAppClick, onCancelClick)
                        }
                        is WithdrawStatus.ManualTransferRequiredIBAN -> {
                            ScreenIBAN(status, onBankAppClick, onCancelClick)
                        }
                    }
                }

            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.withdraw_title)
    }
}
