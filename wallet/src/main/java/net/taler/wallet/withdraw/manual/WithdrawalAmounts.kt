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

package net.taler.wallet.withdraw.manual

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import net.taler.wallet.transactions.WithdrawalExchangeAccountDetails

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

@Composable
fun TransferAccountChooser(
    modifier: Modifier = Modifier,
    accounts: List<WithdrawalExchangeAccountDetails>,
    selectedAccount: WithdrawalExchangeAccountDetails,
    onSelectAccount: (account: WithdrawalExchangeAccountDetails) -> Unit,
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
            items(count = accounts.size) { index ->
                val account = accounts[index]
                SelectionChip(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    label = {
                            if (account.currencySpecification?.name != null) {
                                Text(stringResource(
                                    R.string.withdraw_account_currency,
                                    index + 1,
                                    account.currencySpecification.name,
                                ))
                            } else if (account.transferAmount?.currency != null) {
                                Text(stringResource(
                                    R.string.withdraw_account_currency,
                                    index + 1,
                                    account.transferAmount.currency,
                                ))
                            } else Text(stringResource(R.string.withdraw_account, index + 1))
                    },
                    selected = account.paytoUri == selectedAccount.paytoUri,
                    value = account.paytoUri,
                    onSelected = { value ->
                        accounts.find { it.paytoUri == value }?.let {
                            onSelectAccount(it)
                        }
                    }
                )
            }
        }
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