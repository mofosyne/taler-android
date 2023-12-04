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
import net.taler.wallet.currency.CurrencySpecification
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.WithdrawalExchangeAccountDetails

@Composable
fun ConversionComposable(
    amountRaw: Amount,
    amountEffective: Amount,
    accounts: List<WithdrawalExchangeAccountDetails>?,
) {
    val altCurrencies = accounts
        ?.filter { it.currencySpecification != null }
        ?.map { it.currencySpecification!!.name } ?: emptyList()
    var selectedCurrency by remember { mutableStateOf(amountRaw.currency) }
    val selectedAccount = accounts?.find {
        it.currencySpecification?.name == selectedCurrency
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (altCurrencies.isNotEmpty()) {
            TransferCurrencyChooser(
                currencies = listOf(amountRaw.currency) + altCurrencies,
                selectedCurrency = selectedCurrency,
                onSelectedCurrency = { selectedCurrency = it }
            )
        }

        selectedAccount?.transferAmount?.let { transferAmount ->
            TransactionAmountComposable(
                label = "Transfer",
                amount = transferAmount,
                amountType = AmountType.Neutral,
            )
        }

        TransactionAmountComposable(
            label = if (selectedCurrency == amountRaw.currency) {
                stringResource(R.string.amount_chosen)
            } else {
                "Conversion"
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
    }
}

@Composable
fun TransferCurrencyChooser(
    modifier: Modifier = Modifier,
    currencies: List<String>,
    selectedCurrency: String,
    onSelectedCurrency: (currency: String) -> Unit,
) {
    if (currencies.isEmpty()) return
    val currencyOptions: List<String> = currencies.distinct()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = "This exchange allows currency conversion.",
            style = MaterialTheme.typography.bodyMedium,
        )

        LazyRow {
            items(items = currencyOptions) { currency ->
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

@Preview
@Composable
fun ConversionComposablePreview() {
    Surface {
        ConversionComposable(
            amountRaw = Amount.fromJSONString("CHF:10"),
            amountEffective = Amount.fromJSONString("CHF:9.5"),
            accounts = listOf(
                WithdrawalExchangeAccountDetails(
                    paytoUri = "payto://IBAN/1231231231",
                    transferAmount = Amount.fromJSONString("NETZBON:10"),
                    currencySpecification = CurrencySpecification(
                        name = "NETZBON",
                        numFractionalInputDigits = 2,
                        numFractionalNormalDigits = 2,
                        numFractionalTrailingZeroDigits = 2,
                        altUnitNames = mapOf("0" to "NETZBON"),
                    ),
                ),
            ),
        )
    }
}