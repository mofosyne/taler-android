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
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.lib.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.databinding.FragmentPromptWithdrawBinding
import net.taler.wallet.withdraw.WithdrawStatus.Loading
import net.taler.wallet.withdraw.WithdrawStatus.TosReviewRequired
import net.taler.wallet.withdraw.WithdrawStatus.Withdrawing

class PromptWithdrawFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var ui: FragmentPromptWithdrawBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentPromptWithdrawBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, {
            showWithdrawStatus(it)
        })
    }

    private fun showWithdrawStatus(status: WithdrawStatus?): Any = when (status) {
        is WithdrawStatus.ReceivedDetails -> {
            showContent(status.amountRaw, status.amountEffective, status.exchangeBaseUrl)
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
        is WithdrawStatus.Success -> {
            model.showProgressBar.value = false
            withdrawManager.withdrawStatus.value = null
            findNavController().navigate(R.id.action_promptWithdraw_to_nav_main)
            model.showTransactions(status.currency)
            Snackbar.make(requireView(), R.string.withdraw_initiated, LENGTH_LONG).show()
        }
        is Loading -> {
            model.showProgressBar.value = true
        }
        is Withdrawing -> {
            model.showProgressBar.value = true
        }
        is TosReviewRequired -> {
            showContent(status.amountRaw, status.amountEffective, status.exchangeBaseUrl)
            ui.confirmWithdrawButton.apply {
                text = getString(R.string.withdraw_button_tos)
                setOnClickListener {
                    findNavController().navigate(R.id.action_promptWithdraw_to_reviewExchangeTOS)
                }
                isEnabled = true
            }
        }
        is WithdrawStatus.Error -> {
            model.showProgressBar.value = false
            findNavController().navigate(R.id.action_promptWithdraw_to_errorFragment)
        }
        null -> model.showProgressBar.value = false
    }

    private fun showContent(amountRaw: Amount, amountEffective: Amount, exchange: String) {
        model.showProgressBar.value = false
        ui.progressBar.fadeOut()

        ui.introView.fadeIn()
        ui.effectiveAmountView.text = amountEffective.toString()
        ui.effectiveAmountView.fadeIn()

        ui.chosenAmountLabel.fadeIn()
        ui.chosenAmountView.text = amountRaw.toString()
        ui.chosenAmountView.fadeIn()

        ui.feeLabel.fadeIn()
        ui.feeView.text = getString(R.string.amount_negative, (amountRaw - amountEffective).toString())
        ui.feeView.fadeIn()

        ui.exchangeIntroView.fadeIn()
        ui.withdrawExchangeUrl.text = cleanExchange(exchange)
        ui.withdrawExchangeUrl.fadeIn()
        ui.selectExchangeButton.fadeIn()
        ui.selectExchangeButton.setOnClickListener {
            Toast.makeText(context, "Not yet implemented", LENGTH_SHORT).show()
        }

        ui.withdrawCard.fadeIn()
    }

}
