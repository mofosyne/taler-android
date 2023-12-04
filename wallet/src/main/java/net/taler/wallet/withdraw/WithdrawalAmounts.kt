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

package net.taler.wallet.withdraw

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.compose.SelectionChip
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.TransactionAmountComposable

@Composable
fun WithdrawalAmounts(
    amountRaw: Amount,
    amountEffective: Amount,
    conversionAmounts: List<Amount>? = null,
    defaultCurrency: String? = null,
) {
    // TODO: use currencySpecification.name here!
    val currencies = (conversionAmounts?.map { it.currency } ?: emptyList()).toSet() + amountRaw.currency
    var selectedCurrency by remember {
        mutableStateOf(defaultCurrency ?: amountRaw.currency)
    }
    val selectedConversion = conversionAmounts?.find {
        (defaultCurrency ?: it.currency) == selectedCurrency
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (currencies.size > 1) {
            TransferCurrencyChooser(
                currencies = currencies,
                selectedCurrency = selectedCurrency,
                onSelectedCurrency = { selectedCurrency = it },
            )
        }

        if (selectedCurrency != amountRaw.currency) {
            selectedConversion?.let { transferAmount ->
                TransactionAmountComposable(
                    label = stringResource(R.string.withdraw_transfer),
                    amount = transferAmount,
                    amountType = AmountType.Neutral,
                )
            }
        }

        TransactionAmountComposable(
            label = if (selectedCurrency == amountRaw.currency) {
                stringResource(R.string.amount_chosen)
            } else {
                stringResource(R.string.withdraw_conversion)
            },
            amount = amountRaw,
            amountType = AmountType.Neutral,
        )

        val fee = amountRaw - amountEffective
        if (!fee.isZero()) {
            TransactionAmountComposable(
                label = stringResource(id = R.string.withdraw_fees),
                amount = fee,
                amountType = AmountType.Negative,
            )
        }

        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_total),
            amount = amountEffective,
            amountType = AmountType.Positive,
        )
    }
}

@Composable
fun TransferCurrencyChooser(
    modifier: Modifier = Modifier,
    currencies: Set<String>,
    selectedCurrency: String,
    onSelectedCurrency: (currency: String) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.withdraw_conversion_support),
            style = MaterialTheme.typography.bodyMedium,
        )

        LazyRow {
            items(items = currencies.toList()) { currency ->
                SelectionChip(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    label = { Text(currency) },
                    selected = currency == selectedCurrency,
                    value = currency,
                    onSelected = onSelectedCurrency,
                )
            }
        }
    }
}

@Composable
fun WithdrawalAmountTransfer(
    amountRaw: Amount,
    amountEffective: Amount,
    conversionAmountRaw: Amount,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TransactionAmountComposable(
            label = stringResource(R.string.withdraw_transfer),
            amount = conversionAmountRaw,
            amountType = AmountType.Neutral,
        )

        if (amountRaw.currency != conversionAmountRaw.currency) {
            TransactionAmountComposable(
                label = stringResource(R.string.withdraw_conversion),
                amount = amountRaw,
                amountType = AmountType.Neutral,
            )
        }

        val fee = amountRaw - amountEffective
        if (!fee.isZero()) {
            TransactionAmountComposable(
                label = stringResource(id = R.string.withdraw_fees),
                amount = fee,
                amountType = AmountType.Negative,
            )
        }

        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_total),
            amount = amountEffective,
            amountType = AmountType.Positive,
        )
    }
}

@Preview
@Composable
fun WithdrawalAmountsPreview() {
    Surface {
        WithdrawalAmounts(
            amountRaw = Amount.fromJSONString("CHF:10"),
            amountEffective = Amount.fromJSONString("CHF:9.5"),
            conversionAmounts = listOf(
                Amount.fromJSONString("NETZBON:10"),
            ),
        )
    }
}

@Preview
@Composable
fun WithdrawalAmountTransferPreview() {
    Surface {
        WithdrawalAmountTransfer(
            amountRaw = Amount.fromJSONString("CHF:10"),
            amountEffective = Amount.fromJSONString("CHF:9.5"),
            conversionAmountRaw = Amount.fromJSONString("NETZBON:10"),
        )
    }
}