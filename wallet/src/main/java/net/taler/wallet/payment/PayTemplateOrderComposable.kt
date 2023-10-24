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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun PayTemplateOrderComposable(
    currencies: List<String>, // assumed to have size > 0
    defaultSummary: String? = null,
    amountStatus: AmountFieldStatus,
    onCreateAmount: (String, String) -> AmountResult,
    onError: (msgRes: Int) -> Unit,
    onSubmit: (summary: String?, amount: Amount?) -> Unit,
) {
    val amountDefault = amountStatus as? AmountFieldStatus.Default

    var summary by remember { mutableStateOf(defaultSummary) }
    var currency by remember { mutableStateOf(amountDefault?.currency ?: currencies[0]) }
    var amount by remember { mutableStateOf(amountDefault?.amountStr ?: "0") }

    Column(horizontalAlignment = End) {
        if (defaultSummary != null) OutlinedTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = summary ?: "",
            isError = summary.isNullOrBlank(),
            onValueChange = { summary = it },
            singleLine = true,
            label = { Text(stringResource(R.string.withdraw_manual_ready_subject)) },
        )
        if (amountDefault != null) AmountField(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            amount = amount,
            currency = currency,
            currencies = currencies,
            fixedCurrency = (amountStatus as? AmountFieldStatus.Default)?.currency != null,
            onAmountChosen = { a, c ->
                amount = a
                currency = c
            },
        )
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = defaultSummary == null || !summary.isNullOrBlank(),
            onClick = {
                when (val res = onCreateAmount(amount, currency)) {
                    is AmountResult.InsufficientBalance -> onError(R.string.payment_balance_insufficient)
                    is AmountResult.InvalidAmount -> onError(R.string.receive_amount_invalid)
                    is AmountResult.Success -> onSubmit(summary, res.amount)
                }
            },
        ) {
            Text(stringResource(R.string.payment_create_order))
        }
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
fun PayTemplateDefaultPreview() {
    TalerSurface {
        PayTemplateOrderComposable(
            defaultSummary = "Donation",
            amountStatus = AmountFieldStatus.Default("20", "ARS"),
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { },
        )
    }
}

@Preview
@Composable
fun PayTemplateFixedAmountPreview() {
    TalerSurface {
        PayTemplateOrderComposable(
            defaultSummary = "default summary",
            amountStatus = AmountFieldStatus.FixedAmount,
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { },
        )
    }
}

@Preview
@Composable
fun PayTemplateBlankSubjectPreview() {
    TalerSurface {
        PayTemplateOrderComposable(
            defaultSummary = "",
            amountStatus = AmountFieldStatus.FixedAmount,
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { },
        )
    }
}
