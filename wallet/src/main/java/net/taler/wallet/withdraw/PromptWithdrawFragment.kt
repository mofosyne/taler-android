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
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import kotlinx.android.synthetic.main.fragment_prompt_withdraw.*
import net.taler.common.Amount
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.withdraw.WithdrawStatus.Loading
import net.taler.wallet.withdraw.WithdrawStatus.TermsOfServiceReviewRequired
import net.taler.wallet.withdraw.WithdrawStatus.Withdrawing

class PromptWithdrawFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prompt_withdraw, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, Observer {
            showWithdrawStatus(it)
        })
    }

    private fun showWithdrawStatus(status: WithdrawStatus?): Any = when (status) {
        is WithdrawStatus.ReceivedDetails -> {
            showContent(status.amount, status.fee, status.exchange)
            confirmWithdrawButton.apply {
                text = getString(R.string.withdraw_button_confirm)
                setOnClickListener {
                    it.fadeOut()
                    confirmProgressBar.fadeIn()
                    withdrawManager.acceptWithdrawal(
                        status.talerWithdrawUri,
                        status.exchange,
                        status.amount.currency
                    )
                }
                isEnabled = true
            }
        }
        is WithdrawStatus.Success -> {
            model.showProgressBar.value = false
            withdrawManager.withdrawStatus.value = null
            // TODO bring the user to the currency's transaction page, if there's more than one currency
            model.transactionManager.selectedCurrency = status.currency
            findNavController().navigate(R.id.action_promptWithdraw_to_nav_main)
            Snackbar.make(requireView(), R.string.withdraw_initiated, LENGTH_LONG).show()
        }
        is Loading -> {
            model.showProgressBar.value = true
        }
        is Withdrawing -> {
            model.showProgressBar.value = true
        }
        is TermsOfServiceReviewRequired -> {
            showContent(status.amount, status.fee, status.exchange)
            confirmWithdrawButton.apply {
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

    private fun showContent(amount: Amount, fee: Amount, exchange: String) {
        model.showProgressBar.value = false
        progressBar.fadeOut()

        introView.fadeIn()
        effectiveAmountView.text = (amount - fee).toString()
        effectiveAmountView.fadeIn()

        chosenAmountLabel.fadeIn()
        chosenAmountView.text = amount.toString()
        chosenAmountView.fadeIn()

        feeLabel.fadeIn()
        feeView.text = getString(R.string.amount_negative, fee.toString())
        feeView.fadeIn()

        exchangeIntroView.fadeIn()
        withdrawExchangeUrl.text = cleanExchange(exchange)
        withdrawExchangeUrl.fadeIn()
        selectExchangeButton.fadeIn()
        selectExchangeButton.setOnClickListener {
            findNavController().navigate(R.id.action_promptWithdraw_to_selectExchangeFragment)
        }

        withdrawCard.fadeIn()
    }

}
