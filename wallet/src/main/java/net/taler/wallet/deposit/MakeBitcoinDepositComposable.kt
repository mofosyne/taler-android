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

package net.taler.wallet.deposit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.taler.common.Amount
import net.taler.wallet.CURRENCY_BTC
import net.taler.wallet.R
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.TransactionAmountComposable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeBitcoinDepositComposable(
    state: DepositState,
    amount: Amount,
    bitcoinAddress: String? = null,
    onMakeDeposit: (Amount, String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        var address by rememberSaveable { mutableStateOf(bitcoinAddress ?: "") }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp)
                .focusRequester(focusRequester),
            value = address,
            enabled = !state.showFees,
            onValueChange = { input ->
                address = input
            },
            isError = address.isBlank(),
            label = {
                Text(
                    stringResource(R.string.send_deposit_bitcoin_address),
                    color = if (address.isBlank()) {
                        MaterialTheme.colorScheme.error
                    } else Color.Unspecified,
                )
            }
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        val amountTitle = if (state.effectiveDepositAmount == null) {
            R.string.amount_chosen
        } else R.string.send_deposit_amount_effective
        TransactionAmountComposable(
            label = stringResource(id = amountTitle),
            amount = state.effectiveDepositAmount ?: amount,
            amountType = AmountType.Positive,
        )
        AnimatedVisibility(visible = state.showFees) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = CenterHorizontally,
            ) {
                val totalAmount = state.totalDepositCost ?: amount
                val effectiveAmount = state.effectiveDepositAmount ?: Amount.zero(amount.currency)
                val fee = totalAmount - effectiveAmount
                TransactionAmountComposable(
                    label = stringResource(id = R.string.withdraw_fees),
                    amount = fee,
                    amountType = AmountType.Negative,
                )
                TransactionAmountComposable(
                    label = stringResource(id = R.string.send_amount),
                    amount = totalAmount,
                    amountType = AmountType.Positive,
                )
            }
        }
        AnimatedVisibility(visible = state is DepositState.Error) {
            Text(
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error,
                text = (state as? DepositState.Error)?.error?.userFacingMsg ?: "",
            )
        }
        val focusManager = LocalFocusManager.current
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = address.isNotBlank(),
            onClick = {
                focusManager.clearFocus()
                // TODO validate bitcoin address
                onMakeDeposit(amount, address)
            },
        ) {
            Text(text = stringResource(
                if (state.showFees) R.string.send_deposit_bitcoin_create_button
                else R.string.send_deposit_check_fees_button
            ))
        }
    }
}

@Preview
@Composable
fun PreviewMakeBitcoinDepositComposable() {
    Surface {
        val state = DepositState.FeesChecked(
            effectiveDepositAmount = Amount.fromString(CURRENCY_BTC, "42.00"),
            totalDepositCost = Amount.fromString(CURRENCY_BTC, "42.23"),
        )
        MakeBitcoinDepositComposable(
            state = state,
            amount = Amount.fromString(CURRENCY_BTC, "42.23")) { _, _ ->
        }
    }
}
