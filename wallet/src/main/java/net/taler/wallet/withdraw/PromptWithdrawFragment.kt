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
import kotlinx.android.synthetic.main.fragment_prompt_withdraw.*
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.R
import net.taler.wallet.WalletViewModel
import net.taler.wallet.withdraw.WithdrawStatus.Loading
import net.taler.wallet.withdraw.WithdrawStatus.TermsOfServiceReviewRequired
import net.taler.wallet.withdraw.WithdrawStatus.Withdrawing

class PromptWithdrawFragment : Fragment() {

    private val model: WalletViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prompt_withdraw, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        confirmWithdrawButton.setOnClickListener {
            val status = withdrawManager.withdrawStatus.value
            if (status !is WithdrawStatus.ReceivedDetails) throw AssertionError()
            it.fadeOut()
            confirmProgressBar.fadeIn()
            withdrawManager.acceptWithdrawal(status.talerWithdrawUri, status.suggestedExchange)
        }

        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, Observer {
            showWithdrawStatus(it)
        })
    }

    private fun showWithdrawStatus(status: WithdrawStatus?) = when (status) {
        is WithdrawStatus.ReceivedDetails -> {
            model.showProgressBar.value = false
            progressBar.fadeOut()

            introView.fadeIn()
            withdrawAmountView.text = status.amount.toString()
            withdrawAmountView.fadeIn()
            feeView.fadeIn()

            exchangeIntroView.fadeIn()
            withdrawExchangeUrl.text = status.suggestedExchange
            withdrawExchangeUrl.fadeIn()

            confirmWithdrawButton.isEnabled = true
        }
        is WithdrawStatus.Success -> {
            model.showProgressBar.value = false
            withdrawManager.withdrawStatus.value = null
            findNavController().navigate(R.id.action_promptWithdraw_to_withdrawSuccessful)
        }
        is Loading -> {
            model.showProgressBar.value = true
        }
        is Withdrawing -> {
            model.showProgressBar.value = true
        }
        is TermsOfServiceReviewRequired -> {
            model.showProgressBar.value = false
            findNavController().navigate(R.id.action_promptWithdraw_to_reviewExchangeTOS)
        }
        is WithdrawStatus.Error -> {
            model.showProgressBar.value = false
            findNavController().navigate(R.id.action_promptWithdraw_to_errorFragment)
        }
        null -> model.showProgressBar.value = false
    }

}
