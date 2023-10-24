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
import net.taler.wallet.R
import net.taler.wallet.transactions.AmountType.Negative
import net.taler.wallet.transactions.AmountType.Positive
import net.taler.wallet.transactions.TransactionAmountComposable

@Composable
fun MakeDepositComposable(
    state: DepositState,
    amount: Amount,
    presetName: String? = null,
    presetIban: String? = null,
    onMakeDeposit: (Amount, String, String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        var name by rememberSaveable { mutableStateOf(presetName ?: "") }
        var iban by rememberSaveable { mutableStateOf(presetIban ?: "") }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp)
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = name,
            enabled = !state.showFees,
            onValueChange = { input ->
                name = input
            },
            isError = name.isBlank(),
            label = {
                Text(
                    stringResource(R.string.send_deposit_name),
                    color = if (name.isBlank()) {
                        MaterialTheme.colorScheme.error
                    } else Color.Unspecified,
                )
            }
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        val ibanError = state is DepositState.IbanInvalid
        OutlinedTextField(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            value = iban,
            enabled = !state.showFees,
            onValueChange = { input ->
                iban = input.uppercase()
            },
            isError = ibanError,
            supportingText = {
                if (ibanError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.send_deposit_iban_error),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            label = {
                Text(
                    text = stringResource(R.string.send_deposit_iban),
                    color = if (ibanError) {
                        MaterialTheme.colorScheme.error
                    } else Color.Unspecified,
                )
            }
        )
        TransactionAmountComposable(
            label = if (state.effectiveDepositAmount == null) {
                stringResource(R.string.amount_chosen)
            } else {
                stringResource(R.string.send_deposit_amount_effective)
            },
            amount = state.effectiveDepositAmount ?: amount,
            amountType = Positive,
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
                    label = stringResource(R.string.withdraw_fees),
                    amount = fee,
                    amountType = if (fee.isZero()) Positive else Negative,
                )

                TransactionAmountComposable(
                    label = stringResource(R.string.send_amount),
                    amount = totalAmount,
                    amountType = Positive,
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
            enabled = iban.isNotBlank(),
            onClick = {
                focusManager.clearFocus()
                onMakeDeposit(amount, name, iban)
            },
        ) {
            Text(
                text = stringResource(
                    if (state is DepositState.FeesChecked) R.string.send_deposit_create_button
                    else R.string.send_deposit_check_fees_button
                )
            )
        }
    }
}

@Preview
@Composable
fun PreviewMakeDepositComposable() {
    Surface {
        val state = DepositState.FeesChecked(
            effectiveDepositAmount = Amount.fromString("TESTKUDOS", "42.00"),
            totalDepositCost = Amount.fromString("TESTKUDOS", "42.23"),
        )
        MakeDepositComposable(
            state = state,
            amount = Amount.fromString("TESTKUDOS", "42.23")) { _, _, _ ->
        }
    }
}
