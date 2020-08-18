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

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_manual_withdraw.*
import net.taler.common.hideKeyboard
import net.taler.lib.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.scanQrCode
import java.util.Locale

class ManualWithdrawFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val exchangeManager by lazy { model.exchangeManager }
    private val exchangeItem by lazy { requireNotNull(exchangeManager.withdrawalExchange) }
    private val withdrawManager by lazy { model.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_manual_withdraw, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        qrCodeButton.setOnClickListener { scanQrCode(requireActivity()) }
        currencyView.text = exchangeItem.currency
        val paymentOptions = exchangeItem.paytoUris.mapNotNull {paytoUri ->
            Uri.parse(paytoUri).authority?.toUpperCase(Locale.getDefault())
        }.joinToString(separator = "\n", prefix = "• ")
        paymentOptionsLabel.text =
            getString(R.string.withdraw_manual_payment_options, exchangeItem.name, paymentOptions)
        checkFeesButton.setOnClickListener {
            val value = amountView.text.toString().toLong()
            val amount = Amount(exchangeItem.currency, value, 0)
            amountView.hideKeyboard()
            Toast.makeText(view.context, "Not implemented: $amount", LENGTH_SHORT).show()
            withdrawManager.getWithdrawalDetails(exchangeItem.exchangeBaseUrl, amount)
        }
    }

}
