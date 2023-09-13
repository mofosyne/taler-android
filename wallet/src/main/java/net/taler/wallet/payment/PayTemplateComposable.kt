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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.AmountResult
import net.taler.wallet.R
import net.taler.wallet.compose.AmountInputField
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.deposit.CurrencyDropdown

sealed class AmountFieldStatus {
    object FixedAmount: AmountFieldStatus()
    class Default(
        val amountStr: String? = null,
        val currency: String? = null,
    ): AmountFieldStatus()
    object Invalid: AmountFieldStatus()
}

@Composable
fun PayTemplateComposable(
    summary: String?,
    amountStatus: AmountFieldStatus,
    currencies: List<String>,
    payStatus: PayStatus,
    onCreateAmount: (String, String) -> AmountResult,
    onSubmit: (Map<String, String>) -> Unit,
    onError: (resId: Int) -> Unit,
) {

    // If wallet is empty, there's no way the user can pay something
    if (amountStatus is AmountFieldStatus.Invalid) {
        PayTemplateError(stringResource(R.string.receive_amount_invalid))
    } else if (payStatus is PayStatus.InsufficientBalance || currencies.isEmpty()) {
        PayTemplateError(stringResource(R.string.payment_balance_insufficient))
    } else when (payStatus) {
        is PayStatus.None -> PayTemplateDefault(
            currencies = currencies,
            summary = summary,
            amountStatus = amountStatus,
            onCreateAmount = onCreateAmount,
            onError = onError,
            onSubmit = { s, a ->
                onSubmit(mutableMapOf<String, String>().apply {
                    s?.let { put("summary", it) }
                    a?.let { put("amount", it.toJSONString()) }
                })
            }
        )
        is PayStatus.Loading -> PayTemplateLoading()
        is PayStatus.AlreadyPaid -> PayTemplateError(stringResource(R.string.payment_already_paid))

        // TODO we should handle the other cases or explain why we don't handle them
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayTemplateDefault(
    currencies: List<String>,
    summary: String? = null,
    amountStatus: AmountFieldStatus,
    onCreateAmount: (String, String) -> AmountResult,
    onError: (msgRes: Int) -> Unit,
    onSubmit: (summary: String?, amount: Amount?) -> Unit,
) {
    val amountDefault = amountStatus as? AmountFieldStatus.Default

    var localSummary by remember { mutableStateOf(summary) }
    var localAmount by remember { mutableStateOf(
        amountDefault?.let { s -> s.amountStr ?: "0" }
    ) }
    var localCurrency by remember { mutableStateOf(
        amountDefault?.let { s -> s.currency ?: currencies[0] }
    ) }

    Column(horizontalAlignment = End) {
        localSummary?.let { summary ->
            OutlinedTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                value = summary,
                isError = summary.isBlank(),
                onValueChange = { localSummary = it },
                singleLine = true,
                label = { Text(stringResource(R.string.withdraw_manual_ready_subject)) },
            )
        }

        localAmount?.let { amount ->
            localCurrency?.let { currency ->
                AmountField(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    amount = amount,
                    currency = currency,
                    currencies = currencies,
                    fixedCurrency = (amountStatus as? AmountFieldStatus.Default)?.currency != null,
                    onAmountChosen = { a, c ->
                        localAmount = a
                        localCurrency = c
                    },
                )
            }
        }

        Button(
            modifier = Modifier.padding(16.dp),
            enabled = localSummary == null || localSummary!!.isNotBlank(),
            onClick = {
                localAmount?.let { amount ->
                    localCurrency?.let { currency ->
                        when (val res = onCreateAmount(amount, currency)) {
                            is AmountResult.InsufficientBalance -> onError(R.string.payment_balance_insufficient)
                            is AmountResult.InvalidAmount -> onError(R.string.receive_amount_invalid)
                            is AmountResult.Success -> onSubmit(summary, res.amount)
                        }
                    }
                }
            },
        ) {
            Text(stringResource(R.string.payment_create_order))
        }
    }
}

@Composable
fun PayTemplateError(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
fun PayTemplateLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AmountField(
    modifier: Modifier = Modifier,
    currencies: List<String>,
    fixedCurrency: Boolean,
    amount: String,
    currency: String,
    onAmountChosen: (amount: String, currency: String) -> Unit,
) {
    Row(
        modifier = modifier,
    ) {
        AmountInputField(
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f),
            value = amount,
            onValueChange = { onAmountChosen(it, currency) },
            label = { Text(stringResource(R.string.send_amount)) }
        )
        CurrencyDropdown(
            modifier = Modifier.weight(1f),
            initialCurrency = currency,
            currencies = currencies,
            onCurrencyChanged = { onAmountChosen(amount, it) },
            readOnly = fixedCurrency,
        )
    }
}

@Preview
@Composable
fun PayTemplateComposablePreview() {
    TalerSurface {
        PayTemplateComposable(
            summary = "Donation",
            amountStatus = AmountFieldStatus.Default("20",  "ARS"),
            currencies = listOf("KUDOS", "ARS"),
            // TODO create previews for other states
            payStatus = PayStatus.None,
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { },
            onError = { },
        )
    }
}
