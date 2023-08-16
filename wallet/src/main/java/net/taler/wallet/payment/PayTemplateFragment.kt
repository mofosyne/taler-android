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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.AmountParserException
import net.taler.common.showError
import net.taler.wallet.AmountResult
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.deposit.CurrencyDropdown

class PayTemplateFragment: Fragment() {
    private val model: MainViewModel by activityViewModels()
    private var uriString: String? = null
    private var uri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        uriString = arguments?.getString("uri") ?: error("no amount passed")
        uri = Uri.parse(uriString)
        val currencies = model.getCurrencies()

        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    PayTemplateComposable(
                        uri = uri!!,
                        currencies = currencies,
                        fragment = this@PayTemplateFragment,
                        model = model,
                        onSubmit = { createOrder(it) },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO: this is not ideal, if the template is fixed, the
        //  user shouldn't even have to go through this fragment.
        if (uri?.queryParameterNames?.isEmpty() == true) {
            createOrder(emptyMap())
        }
    }

    private fun createOrder(params: Map<String, String>) {
        uriString ?: return
        model.paymentManager.preparePayForTemplate(uriString!!, params,).invokeOnCompletion {
            if (model.paymentManager.payStatus.value is PayStatus.Prepared) {
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_main, true)
                    .build()
                findNavController()
                    .navigate(R.id.action_global_promptPayment, null, navOptions)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayTemplateComposable(
    uri: Uri,
    currencies: List<String>,
    fragment: Fragment,
    model: MainViewModel,
    onSubmit: (Map<String, String>) -> Unit,
) {
    val queryParams = uri.queryParameterNames

    var summary by remember { mutableStateOf(
        if ("summary" in queryParams)
            uri.getQueryParameter("summary") else null,
    ) }

    var amount by remember { mutableStateOf(
        if ("amount" in queryParams) {
            val amount = uri.getQueryParameter("amount")!!
            val parts = amount.split(':')
            when (parts.size) {
                1 -> Amount.fromString(parts[0], "0")
                2 -> Amount.fromString(parts[0], parts[1])
                else -> throw AmountParserException("Invalid Amount Format")
            }
        } else null,
    ) }

    val payStatus by model.paymentManager.payStatus.asFlow()
        .collectAsState(initial = PayStatus.None)

    // If wallet is empty, there's no way the user can pay something
    if (payStatus is PayStatus.InsufficientBalance || currencies.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.payment_balance_insufficient),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else when (payStatus) {
        is PayStatus.None -> {
            Column(horizontalAlignment = Alignment.End) {
                if ("summary" in queryParams) {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        value = summary!!,
                        isError = summary!!.isBlank(),
                        onValueChange = { summary = it },
                        singleLine = true,
                        label = { Text(stringResource(R.string.withdraw_manual_ready_subject)) },
                    )
                }

                if ("amount" in queryParams) {
                    AmountField(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        amount = amount!!,
                        currencies = currencies,
                        onAmountChosen = { amount = it },
                    )
                }

                Button(
                    modifier = Modifier.padding(16.dp),
                    enabled = summary == null || summary!!.isNotBlank(),
                    onClick = {
                        if (amount != null) {
                            val result = model.createAmount(
                                amount!!.amountStr,
                                amount!!.currency,
                            )
                            when (result) {
                                AmountResult.InsufficientBalance -> {
                                    fragment.showError(R.string.payment_balance_insufficient)
                                }
                                AmountResult.InvalidAmount -> {
                                    fragment.showError(R.string.receive_amount_invalid)
                                }
                                else -> {
                                    onSubmit(
                                        mutableMapOf<String, String>().apply {
                                            if (summary != null) put("summary", summary!!)
                                            if (amount != null) put("amount", amount!!.toJSONString())
                                        }
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(stringResource(R.string.payment_create_order))
                }
            }
        }
        is PayStatus.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }
        is PayStatus.AlreadyPaid -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.payment_already_paid),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        else -> {}
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AmountField(
    modifier: Modifier = Modifier,
    currencies: List<String>,
    amount: Amount,
    onAmountChosen: (Amount) -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        val amountText = if (amount.value == 0L) "" else amount.value.toString()
        val currency = currencies.find { amount.currency == it } ?: currencies[0]
        OutlinedTextField(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            value = amountText,
            placeholder = { Text("0") },
            onValueChange = { input ->
                if (input.isNotBlank()) {
                    onAmountChosen(Amount.fromString(currency, input))
                } else {
                    onAmountChosen(Amount.zero(currency))
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            label = { Text(stringResource(R.string.send_amount)) },
        )
        CurrencyDropdown(
            modifier = Modifier.weight(1f),
            initialCurrency = currency,
            currencies = currencies,
            onCurrencyChanged = { c ->
                onAmountChosen(Amount.fromString(c, amount.amountStr))
            },
        )
    }
}