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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.showError
import net.taler.wallet.AmountResult
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
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

        val queryParams = uri.queryParameterNames

        val summary = if ("summary" in queryParams)
            uri.getQueryParameter("summary")!! else null

        val amountResult = if ("amount" in queryParams) {
            val amount = uri.getQueryParameter("amount")!!
            val parts = amount.split(':')
            when (parts.size) {
                1 -> AmountResult.Success(Amount.fromString(parts[0], "0"))
                2 -> AmountResult.Success(Amount.fromString(parts[0], parts[1]))
                else -> AmountResult.InvalidAmount
            }
        } else null

        return ComposeView(requireContext()).apply {
            setContent {
                val payStatus by model.paymentManager.payStatus
                    .asFlow()
                    .collectAsState(initial = PayStatus.None)
                TalerSurface {
                    PayTemplateComposable(
                        currencies = model.getCurrencies(),
                        summary = summary,
                        amountResult = amountResult,
                        payStatus = payStatus,
                        onCreateAmount = { text, currency ->
                            model.createAmount(text, currency)
                        },
                        onSubmit = { createOrder(it) },
                        onError = { this@PayTemplateFragment.showError(it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (uri.queryParameterNames?.isEmpty() == true) {
            createOrder(emptyMap())
        }

        model.paymentManager.payStatus.observe(viewLifecycleOwner) { payStatus ->
            when (payStatus) {
                is PayStatus.Prepared -> {
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(R.id.nav_main, true)
                        .build()
                    findNavController()
                        .navigate(R.id.action_global_promptPayment, null, navOptions)
                }
                is PayStatus.Error -> {
                    if (model.devMode.value == true) {
                        showError(payStatus.error)
                    } else {
                        showError(R.string.payment_template_error, payStatus.error.userFacingMsg)
                    }
                }
                else -> {}
            }
        }
    }

    private fun createOrder(params: Map<String, String>) {
        model.paymentManager.preparePayForTemplate(uriString, params)
    }
}