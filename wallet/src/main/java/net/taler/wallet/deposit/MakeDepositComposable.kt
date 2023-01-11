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
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.taler.common.Amount
import net.taler.wallet.R

@Composable
fun MakeDepositComposable(
    state: DepositState,
    amount: Amount,
    presetName: String? = null,
    presetIban: String? = null,
    onMakeDeposit: (Amount, String, String, String) -> Unit,
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
        var bic by rememberSaveable { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp)
                .focusRequester(focusRequester),
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
                        colorResource(R.color.red)
                    } else Color.Unspecified,
                )
            }
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp),
            value = iban,
            enabled = !state.showFees,
            onValueChange = { input ->
                iban = input
            },
            isError = iban.isBlank(),
            label = {
                Text(
                    text = stringResource(R.string.send_deposit_iban),
                    color = if (iban.isBlank()) {
                        colorResource(R.color.red)
                    } else Color.Unspecified,
                )
            }
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(16.dp),
            value = bic,
            enabled = !state.showFees,
            onValueChange = { input ->
                bic = input
            },
            label = {
                Text(
                    text = stringResource(R.string.send_deposit_bic),
                )
            }
        )
        val amountTitle = if (state.effectiveDepositAmount == null) {
            R.string.amount_chosen
        } else R.string.send_deposit_amount_effective
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = amountTitle),
        )
        val shownAmount = if (state.effectiveDepositAmount == null) amount else {
            state.effectiveDepositAmount
        }
        Text(
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            color = colorResource(R.color.green),
            text = shownAmount.toString(),
        )
        AnimatedVisibility(visible = state.showFees) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = CenterHorizontally,
            ) {
                val totalAmount = state.totalDepositCost ?: amount
                val effectiveAmount = state.effectiveDepositAmount ?: Amount.zero(amount.currency)
                val fee = totalAmount - effectiveAmount
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.withdraw_fees),
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    color = colorResource(if (fee.isZero()) R.color.green else R.color.red),
                    text = if (fee.isZero()) {
                        fee.toString()
                    } else {
                        stringResource(R.string.amount_negative, fee.toString())
                    },
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.send_amount),
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    color = colorResource(R.color.green),
                    text = totalAmount.toString(),
                )
            }
        }
        AnimatedVisibility(visible = state is DepositState.Error) {
            Text(
                modifier = Modifier.padding(16.dp),
                fontSize = 18.sp,
                color = colorResource(R.color.red),
                text = (state as? DepositState.Error)?.msg ?: "",
            )
        }
        val focusManager = LocalFocusManager.current
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = iban.isNotBlank(),
            onClick = {
                focusManager.clearFocus()
                onMakeDeposit(amount, name, iban, bic)
            },
        ) {
            Text(text = stringResource(
                if (state.showFees) R.string.send_deposit_create_button
                else R.string.send_deposit_check_fees_button
            ))
        }
    }
}

@Preview
@Composable
fun PreviewMakeDepositComposable() {
    Surface {
        val state = DepositState.FeesChecked(
            effectiveDepositAmount = Amount.fromDouble("TESTKUDOS", 42.00),
            totalDepositCost = Amount.fromDouble("TESTKUDOS", 42.23),
        )
        MakeDepositComposable(
            state = state,
            amount = Amount.fromDouble("TESTKUDOS", 42.23)) { _, _, _, _ ->
        }
    }
}