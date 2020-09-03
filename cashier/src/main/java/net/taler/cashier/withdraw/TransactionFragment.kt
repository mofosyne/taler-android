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

package net.taler.cashier.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.cashier.MainViewModel
import net.taler.cashier.R
import net.taler.cashier.databinding.FragmentTransactionBinding
import net.taler.cashier.withdraw.TransactionFragmentDirections.Companion.actionTransactionFragmentToBalanceFragment
import net.taler.cashier.withdraw.TransactionFragmentDirections.Companion.actionTransactionFragmentToErrorFragment
import net.taler.cashier.withdraw.WithdrawResult.Error
import net.taler.cashier.withdraw.WithdrawResult.InsufficientBalance
import net.taler.cashier.withdraw.WithdrawResult.Success
import net.taler.common.NfcManager
import net.taler.common.exhaustive
import net.taler.common.fadeIn
import net.taler.common.fadeOut

class TransactionFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { viewModel.withdrawManager }
    private val nfcManager = NfcManager()

    private lateinit var ui: FragmentTransactionBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentTransactionBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withdrawManager.withdrawAmount.observe(viewLifecycleOwner, { amount ->
            ui.amountView.text = amount?.toString()
        })
        withdrawManager.withdrawResult.observe(viewLifecycleOwner, { result ->
            onWithdrawResultReceived(result)
        })
        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, { status ->
            onWithdrawStatusChanged(status)
        })

        // change intro text depending on whether NFC is available or not
        val hasNfc = NfcManager.hasNfc(requireContext())
        val intro = if (hasNfc) R.string.transaction_intro_nfc else R.string.transaction_intro
        ui.introView.setText(intro)

        ui.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        if (withdrawManager.withdrawResult.value is Success) {
            NfcManager.start(requireActivity(), nfcManager)
        }
    }

    override fun onStop() {
        super.onStop()
        NfcManager.stop(requireActivity())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            withdrawManager.abort()
        }
    }

    private fun onWithdrawResultReceived(result: WithdrawResult?) {
        if (result != null) {
            ui.progressBar.animate()
                .alpha(0f)
                .withEndAction { ui.progressBar.visibility = INVISIBLE }
                .setDuration(750)
                .start()
        }
        when (result) {
            is InsufficientBalance -> setErrorMsg(getString(R.string.withdraw_error_insufficient_balance))
            is WithdrawResult.Offline -> setErrorMsg(getString(R.string.withdraw_error_offline))
            is Error -> setErrorMsg(result.msg)
            is Success -> {
                // start NFC
                nfcManager.setTagString(result.talerUri)
                NfcManager.start(
                    requireActivity(),
                    nfcManager
                )
                // show QR code
                ui.qrCodeView.alpha = 0f
                ui.qrCodeView.animate()
                    .alpha(1f)
                    .withStartAction {
                        ui.qrCodeView.visibility = VISIBLE
                        ui.qrCodeView.setImageBitmap(result.qrCode)
                    }
                    .setDuration(750)
                    .start()
            }
            null -> return
        }.exhaustive
    }

    private fun setErrorMsg(str: String) {
        val c = getColor(requireContext(), R.color.design_default_color_error)
        ui.introView.setTextColor(c)
        ui.introView.text = str
    }

    private fun onWithdrawStatusChanged(status: WithdrawStatus?): Any = when (status) {
        is WithdrawStatus.SelectionDone -> {
            ui.qrCodeView.fadeOut {
                ui.qrCodeView.setImageResource(R.drawable.ic_arrow)
                ui.qrCodeView.fadeIn()
            }
            ui.introView.fadeOut {
                ui.introView.text = getString(R.string.transaction_intro_scanned)
                ui.introView.fadeIn {
                    ui.confirmButton.isEnabled = true
                    ui.confirmButton.setOnClickListener {
                        withdrawManager.confirm(status.withdrawalId)
                    }
                }
            }
        }
        is WithdrawStatus.Confirming -> {
            ui.confirmButton.isEnabled = false
            ui.qrCodeView.fadeOut()
            ui.progressBar.fadeIn()
        }
        is WithdrawStatus.Success -> {
            withdrawManager.completeTransaction()
            actionTransactionFragmentToBalanceFragment().let {
                findNavController().navigate(it)
            }
        }
        is WithdrawStatus.Aborted -> onError()
        is WithdrawStatus.Error -> onError()
        null -> {
            // no-op
        }
    }

    private fun onError() {
        actionTransactionFragmentToErrorFragment().let {
            findNavController().navigate(it)
        }
    }

}
