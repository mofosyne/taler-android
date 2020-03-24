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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_transaction.*
import net.taler.cashier.MainViewModel
import net.taler.cashier.R
import net.taler.cashier.withdraw.TransactionFragmentDirections.Companion.actionTransactionFragmentToBalanceFragment
import net.taler.cashier.withdraw.TransactionFragmentDirections.Companion.actionTransactionFragmentToErrorFragment
import net.taler.cashier.withdraw.WithdrawResult.Error
import net.taler.cashier.withdraw.WithdrawResult.InsufficientBalance
import net.taler.cashier.withdraw.WithdrawResult.Success
import net.taler.common.NfcManager
import net.taler.common.fadeIn
import net.taler.common.fadeOut

class TransactionFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { viewModel.withdrawManager }
    private val nfcManager = NfcManager()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withdrawManager.withdrawAmount.observe(viewLifecycleOwner, Observer { amount ->
            amountView.text = amount?.toString()
        })
        withdrawManager.withdrawResult.observe(viewLifecycleOwner, Observer { result ->
            onWithdrawResultReceived(result)
        })
        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, Observer { status ->
            onWithdrawStatusChanged(status)
        })

        // change intro text depending on whether NFC is available or not
        val hasNfc = NfcManager.hasNfc(requireContext())
        val intro = if (hasNfc) R.string.transaction_intro_nfc else R.string.transaction_intro
        introView.setText(intro)

        cancelButton.setOnClickListener {
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
            progressBar.animate()
                .alpha(0f)
                .withEndAction { progressBar?.visibility = INVISIBLE }
                .setDuration(750)
                .start()
        }
        when (result) {
            is InsufficientBalance -> {
                val c = getColor(requireContext(), R.color.design_default_color_error)
                introView.setTextColor(c)
                introView.text = getString(R.string.withdraw_error_insufficient_balance)
            }
            is Error -> {
                val c = getColor(requireContext(), R.color.design_default_color_error)
                introView.setTextColor(c)
                introView.text = result.msg
            }
            is Success -> {
                // start NFC
                nfcManager.setTagString(result.talerUri)
                NfcManager.start(
                    requireActivity(),
                    nfcManager
                )
                // show QR code
                qrCodeView.alpha = 0f
                qrCodeView.animate()
                    .alpha(1f)
                    .withStartAction {
                        qrCodeView.visibility = VISIBLE
                        qrCodeView.setImageBitmap(result.qrCode)
                    }
                    .setDuration(750)
                    .start()
            }
        }
    }

    private fun onWithdrawStatusChanged(status: WithdrawStatus?): Any = when (status) {
        is WithdrawStatus.SelectionDone -> {
            qrCodeView.fadeOut {
                qrCodeView?.setImageResource(R.drawable.ic_arrow)
                qrCodeView?.fadeIn()
            }
            introView.fadeOut {
                introView?.text = getString(R.string.transaction_intro_scanned)
                introView?.fadeIn {
                    confirmButton?.isEnabled = true
                    confirmButton?.setOnClickListener {
                        withdrawManager.confirm(status.withdrawalId)
                    }
                }
            }
        }
        is WithdrawStatus.Confirming -> {
            confirmButton.isEnabled = false
            qrCodeView.fadeOut()
            progressBar.fadeIn()
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
