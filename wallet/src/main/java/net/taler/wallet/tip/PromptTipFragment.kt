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

package net.taler.wallet.tip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_LONG
import net.taler.common.Amount
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.databinding.FragmentPromptTipBinding

/**
 * Show a tip and ask the user to accept/decline.
 */
class PromptTipFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val tipManager by lazy { model.tipManager }

    private lateinit var ui: FragmentPromptTipBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentPromptTipBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tipManager.tipStatus.observe(viewLifecycleOwner, ::onPaymentStatusChanged)

    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            // tipManager.abortTip()
        }
    }

    private fun showLoading(show: Boolean) {
        model.showProgressBar.value = show
        if (show) {
            ui.progressBar.fadeIn()
        } else {
            ui.progressBar.fadeOut()
        }
    }

    private fun onPaymentStatusChanged(payStatus: TipStatus) = when (payStatus) {
        is TipStatus.Prepared -> {
            showLoading(false)
            showContent(
                amountRaw = payStatus.tipAmountRaw,
                amountEffective = payStatus.tipAmountEffective,
                exchange = payStatus.exchangeBaseUrl,
                merchant = payStatus.merchantBaseUrl
            )
            ui.confirmWithdrawButton.isEnabled = true
            ui.confirmWithdrawButton.setOnClickListener {
                tipManager.confirmTip(
                    payStatus.walletTipId,
                    payStatus.tipAmountRaw.currency
                )
            }
        }
        is TipStatus.Accepting -> {
            model.showProgressBar.value = true
            ui.confirmProgressBar.fadeIn()
            ui.confirmWithdrawButton.fadeOut()
        }
        is TipStatus.AlreadyAccepted -> {
            showLoading(false)
            tipManager.resetTipStatus()
            findNavController().navigate(R.id.action_promptTip_to_alreadyAccepted)
        }
        is TipStatus.Success -> {
            showLoading(false)
            tipManager.resetTipStatus()
            findNavController().navigate(R.id.action_promptTip_to_nav_main)
            model.showTransactions(payStatus.currency)
            Snackbar.make(requireView(), R.string.tip_received, LENGTH_LONG).show()
        }
        is TipStatus.Error -> {
            showLoading(false)
            ui.introView.text = getString(R.string.payment_error, payStatus.error)
            ui.introView.fadeIn()
        }
        is TipStatus.None -> {
            // No tip active
            showLoading(false)
        }
        is TipStatus.Loading -> {
            // Wait until loaded ...
            showLoading(true)
        }
    }

    private fun showContent(
        amountRaw: Amount,
        amountEffective: Amount,
        exchange: String,
        merchant: String,
    ) {
        model.showProgressBar.value = false
        ui.progressBar.fadeOut()

        ui.effectiveAmountView.text = amountEffective.toString()
        ui.introCard.fadeIn()

        ui.chosenAmountView.text = amountRaw.toString()
        ui.chosenAmountCard.fadeIn()

        ui.feeView.text =
            getString(R.string.amount_negative, (amountRaw - amountEffective).toString())
        ui.feeCard.fadeIn()

        ui.withdrawExchangeUrl.text = cleanExchange(exchange)
        ui.exchangeIntroCard.fadeIn()

        ui.withdrawMerchantUrl.text = cleanExchange(merchant)
        ui.merchantIntoCard.fadeIn()

        ui.withdrawCard.fadeIn()
    }

}
