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

package net.taler.wallet.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import net.taler.common.EventObserver
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.lib.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.databinding.FragmentPromptWithdrawBinding
import net.taler.wallet.withdraw.WithdrawStatus.Loading
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails
import net.taler.wallet.withdraw.WithdrawStatus.TosReviewRequired
import net.taler.wallet.withdraw.WithdrawStatus.Withdrawing

class PromptWithdrawFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var ui: FragmentPromptWithdrawBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        ui = FragmentPromptWithdrawBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, {
            showWithdrawStatus(it)
        })
        withdrawManager.exchangeSelection.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_promptWithdraw_to_selectExchangeFragment)
        })
    }

    private fun showWithdrawStatus(status: WithdrawStatus?): Any = when (status) {
        null -> model.showProgressBar.value = false
        is Loading -> model.showProgressBar.value = true
        is WithdrawStatus.NeedsExchange -> {
            model.showProgressBar.value = false
            val exchangeSelection = status.exchangeSelection.getIfNotConsumed()
            if (exchangeSelection == null) { // already consumed
                findNavController().popBackStack()
            } else {
                withdrawManager.selectExchange(exchangeSelection)
            }
        }
        is TosReviewRequired -> onTosReviewRequired(status)
        is ReceivedDetails -> onReceivedDetails(status)
        is Withdrawing -> model.showProgressBar.value = true
        is WithdrawStatus.Success -> {
            model.showProgressBar.value = false
            withdrawManager.withdrawStatus.value = null
            findNavController().navigate(R.id.action_promptWithdraw_to_nav_main)
            model.showTransactions(status.currency)
            Snackbar.make(requireView(), R.string.withdraw_initiated, LENGTH_LONG).show()
        }
        is WithdrawStatus.Error -> {
            model.showProgressBar.value = false
            findNavController().navigate(R.id.action_promptWithdraw_to_errorFragment)
        }
    }

    private fun onTosReviewRequired(s: TosReviewRequired) {
        model.showProgressBar.value = false
        if (s.showImmediately.getIfNotConsumed() == true) {
            findNavController().navigate(R.id.action_promptWithdraw_to_reviewExchangeTOS)
        } else {
            showContent(s.amountRaw, s.amountEffective, s.exchangeBaseUrl, s.talerWithdrawUri)
            ui.confirmWithdrawButton.apply {
                text = getString(R.string.withdraw_button_tos)
                setOnClickListener {
                    findNavController().navigate(R.id.action_promptWithdraw_to_reviewExchangeTOS)
                }
                isEnabled = true
            }
        }
    }

    private fun onReceivedDetails(s: ReceivedDetails) {
        showContent(s.amountRaw, s.amountEffective, s.exchangeBaseUrl, s.talerWithdrawUri)
        ui.confirmWithdrawButton.apply {
            text = getString(R.string.withdraw_button_confirm)
            setOnClickListener {
                it.fadeOut()
                ui.confirmProgressBar.fadeIn()
                withdrawManager.acceptWithdrawal()
            }
            isEnabled = true
        }
    }

    private fun showContent(
        amountRaw: Amount,
        amountEffective: Amount,
        exchange: String,
        uri: String?,
    ) {
        model.showProgressBar.value = false
        ui.progressBar.fadeOut()

        ui.introView.fadeIn()
        ui.effectiveAmountView.text = amountEffective.toString()
        ui.effectiveAmountView.fadeIn()

        ui.chosenAmountLabel.fadeIn()
        ui.chosenAmountView.text = amountRaw.toString()
        ui.chosenAmountView.fadeIn()

        ui.feeLabel.fadeIn()
        ui.feeView.text =
            getString(R.string.amount_negative, (amountRaw - amountEffective).toString())
        ui.feeView.fadeIn()

        ui.exchangeIntroView.fadeIn()
        ui.withdrawExchangeUrl.text = cleanExchange(exchange)
        ui.withdrawExchangeUrl.fadeIn()

        if (uri != null) {  // no Uri for manual withdrawals
            ui.selectExchangeButton.fadeIn()
            ui.selectExchangeButton.setOnClickListener {
                val exchangeSelection = ExchangeSelection(amountRaw, uri)
                withdrawManager.selectExchange(exchangeSelection)
            }
        }

        ui.withdrawCard.fadeIn()
    }

}
