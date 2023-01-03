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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.hideKeyboard
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentManualWithdrawBinding
import java.util.Locale

class ManualWithdrawFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val exchangeManager by lazy { model.exchangeManager }
    private val exchangeItem by lazy { requireNotNull(exchangeManager.withdrawalExchange) }
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var ui: FragmentManualWithdrawBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentManualWithdrawBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.getString("amount")?.let {
            val amount = Amount.fromJSONString(it)
            ui.amountView.setText(amount.amountStr)
        }

        ui.qrCodeButton.setOnClickListener {
            model.scanCode()
        }
        ui.currencyView.text = exchangeItem.currency
        val paymentOptions = exchangeItem.paytoUris.mapNotNull { paytoUri ->
            Uri.parse(paytoUri).authority?.uppercase(Locale.getDefault())
        }.joinToString(separator = "\n", prefix = "• ")
        ui.paymentOptionsLabel.text =
            getString(R.string.withdraw_manual_payment_options, exchangeItem.name, paymentOptions)
        ui.checkFeesButton.setOnClickListener { onCheckFees() }
    }

    private fun onCheckFees() {
        val currency = exchangeItem.currency
        if (currency == null || ui.amountView.text?.isEmpty() != false) {
            ui.amountLayout.error = getString(R.string.withdraw_amount_error)
            return
        }
        ui.amountLayout.error = null
        val value: Double
        try {
            value = ui.amountView.text.toString().replace(',', '.').toDouble()
        } catch (e: NumberFormatException) {
            ui.amountLayout.error = getString(R.string.withdraw_amount_error)
            return
        }
        val amount = Amount.fromDouble(currency, value)
        ui.amountView.hideKeyboard()

        withdrawManager.getWithdrawalDetails(exchangeItem.exchangeBaseUrl, amount)
        findNavController().navigate(R.id.action_nav_exchange_manual_withdrawal_to_promptWithdraw)
    }

}
