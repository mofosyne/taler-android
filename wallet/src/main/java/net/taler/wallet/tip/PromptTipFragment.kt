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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentPromptTipBinding.inflate(inflater, container, false)
        ui.introView.fadeIn()
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

    private fun onPaymentStatusChanged(payStatus: TipStatus?) {
        when (payStatus) {
            null -> {}
            is TipStatus.Prepared -> {
                showLoading(false)
                showContent(
                    amountRaw = payStatus.tipAmountRaw,
                    amountEffective = payStatus.tipAmountEffective,
                    exchange = payStatus.exchangeBaseUrl,
                    merchant = payStatus.merchantBaseUrl
                )
                //showOrder(payStatus.contractTerms, payStatus.amountRaw, fees)
                ui.confirmWithdrawButton.isEnabled = true
                ui.confirmWithdrawButton.setOnClickListener {
                    model.showProgressBar.value = true
                    tipManager.confirmTip(
                        payStatus.walletTipId,
                        payStatus.tipAmountRaw.currency
                    )
                    ui.confirmWithdrawButton.fadeOut()
                    ui.progressBar.fadeIn()
                }
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
                // No payment active.
                showLoading(false)
            }
            is TipStatus.Loading -> {
                // Wait until loaded ...
                showLoading(true)
            }
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

        ui.merchantIntroView.fadeIn()
        ui.withdrawMerchantUrl.text = cleanExchange(merchant)
        ui.withdrawMerchantUrl.fadeIn()

        ui.withdrawCard.fadeIn()
    }

}
