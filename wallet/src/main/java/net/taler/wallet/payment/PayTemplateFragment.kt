/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.wallet.payment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.showError
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.showError

class PayTemplateFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private lateinit var uriString: String
    private lateinit var uri: Uri

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        uriString = arguments?.getString("uri") ?: error("no amount passed")
        uri = Uri.parse(uriString)

        val defaultSummary = uri.getQueryParameter("summary")
        val defaultAmount = uri.getQueryParameter("amount")
        val amountFieldStatus = getAmountFieldStatus(defaultAmount)

        val payStatusFlow = model.paymentManager.payStatus.asFlow()

        return ComposeView(requireContext()).apply {
            setContent {
                val payStatus = payStatusFlow.collectAsStateLifecycleAware(initial = PayStatus.None)
                TalerSurface {
                    PayTemplateComposable(
                        currencies = model.getCurrencies(),
                        defaultSummary = defaultSummary,
                        amountStatus = amountFieldStatus,
                        payStatus = payStatus.value,
                        onCreateAmount = model::createAmount,
                        onSubmit = this@PayTemplateFragment::createOrder,
                        onError = { this@PayTemplateFragment.showError(it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (uri.queryParameterNames?.isEmpty() == true) {
            createOrder(null, null)
        }

        model.paymentManager.payStatus.observe(viewLifecycleOwner) { payStatus ->
            when (payStatus) {
                is PayStatus.Prepared -> {
                    findNavController().navigate(R.id.action_promptPayTemplate_to_promptPayment)
                }

                is PayStatus.Pending -> if (payStatus.error != null && model.devMode.value == true) {
                    showError(payStatus.error)
                }

                else -> {}
            }
        }
    }

    private fun getAmountFieldStatus(defaultAmount: String?): AmountFieldStatus {
        return if (defaultAmount == null) {
            AmountFieldStatus.FixedAmount
        } else if (defaultAmount.isBlank()) {
            AmountFieldStatus.Default()
        } else {
            val parts = defaultAmount.split(":")
            when (parts.size) {
                0 -> AmountFieldStatus.Default()
                1 -> AmountFieldStatus.Default(currency = parts[0])
                2 -> AmountFieldStatus.Default(parts[1], parts[0])
                else -> AmountFieldStatus.Invalid
            }
        }
    }

    private fun createOrder(summary: String?, amount: Amount?) {
        model.paymentManager.preparePayForTemplate(uriString, summary, amount)
    }
}
