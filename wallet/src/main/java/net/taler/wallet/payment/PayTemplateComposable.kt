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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.wallet.AmountResult
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface

sealed class AmountFieldStatus {
    object FixedAmount : AmountFieldStatus()
    class Default(
        val amountStr: String? = null,
        val currency: String? = null,
    ) : AmountFieldStatus()

    object Invalid : AmountFieldStatus()
}

@Composable
fun PayTemplateComposable(
    defaultSummary: String?,
    amountStatus: AmountFieldStatus,
    currencies: List<String>,
    payStatus: PayStatus,
    onCreateAmount: (String, String) -> AmountResult,
    onSubmit: (summary: String?, amount: Amount?) -> Unit,
    onError: (resId: Int) -> Unit,
) {
    // If wallet is empty, there's no way the user can pay something
    if (amountStatus is AmountFieldStatus.Invalid) {
        PayTemplateError(stringResource(R.string.receive_amount_invalid))
    } else if (currencies.isEmpty()) {
        PayTemplateError(stringResource(R.string.payment_balance_insufficient))
    } else when (payStatus) {
        is PayStatus.None -> PayTemplateOrderComposable(
            currencies = currencies,
            defaultSummary = defaultSummary,
            amountStatus = amountStatus,
            onCreateAmount = onCreateAmount,
            onError = onError,
            onSubmit = onSubmit,
        )

        is PayStatus.Loading -> PayTemplateLoading()
        is PayStatus.AlreadyPaid -> PayTemplateError(stringResource(R.string.payment_already_paid))
        is PayStatus.InsufficientBalance -> PayTemplateError(stringResource(R.string.payment_balance_insufficient))
        is PayStatus.Error -> {} // handled in fragment will show bottom sheet FIXME white page?
        is PayStatus.Prepared -> {} // handled in fragment, will redirect
        is PayStatus.Success -> {} // handled by other UI flow, no need for content here
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

@Preview
@Composable
fun PayTemplateLoadingPreview() {
    TalerSurface {
        PayTemplateComposable(
            defaultSummary = "Donation",
            amountStatus = AmountFieldStatus.Default("20", "ARS"),
            payStatus = PayStatus.Loading,
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { _ -> },
        )
    }
}

@Preview
@Composable
fun PayTemplateInsufficientBalancePreview() {
    TalerSurface {
        PayTemplateComposable(
            defaultSummary = "Donation",
            amountStatus = AmountFieldStatus.Default("20", "ARS"),
            payStatus = PayStatus.InsufficientBalance(
                ContractTerms(
                    "test",
                    amount = Amount.zero("TESTKUDOS"),
                    products = emptyList()
                ), Amount.zero("TESTKUDOS")
            ),
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { _ -> },
        )
    }
}

@Preview
@Composable
fun PayTemplateAlreadyPaidPreview() {
    TalerSurface {
        PayTemplateComposable(
            defaultSummary = "Donation",
            amountStatus = AmountFieldStatus.Default("20", "ARS"),
            payStatus = PayStatus.AlreadyPaid,
            currencies = listOf("KUDOS", "ARS"),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { _ -> },
        )
    }
}


@Preview
@Composable
fun PayTemplateNoCurrenciesPreview() {
    TalerSurface {
        PayTemplateComposable(
            defaultSummary = "Donation",
            amountStatus = AmountFieldStatus.Default("20", "ARS"),
            payStatus = PayStatus.None,
            currencies = emptyList(),
            onCreateAmount = { text, currency ->
                AmountResult.Success(amount = Amount.fromString(currency, text))
            },
            onSubmit = { _, _ -> },
            onError = { _ -> },
        )
    }
}
