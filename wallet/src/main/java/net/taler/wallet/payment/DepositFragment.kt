/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.collectAsStateLifecycleAware

class DepositFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val paymentManager get() = model.paymentManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val amount = arguments?.getString("amount")?.let {
            Amount.fromJSONString(it)
        } ?: error("no amount passed")

        return ComposeView(requireContext()).apply {
            setContent {
                MdcTheme {
                    Surface {
                        val state = paymentManager.depositState.collectAsStateLifecycleAware()
                        MakeDepositComposable(
                            state = state.value,
                            amount = amount,
                            onMakeDeposit = this@DepositFragment::onDepositButtonClicked,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.send_deposit_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            paymentManager.resetDepositState()
        }
    }

    private fun onDepositButtonClicked(
        amount: Amount,
        receiverName: String,
        iban: String,
        bic: String,
    ) {
        paymentManager.onDepositButtonClicked(amount, receiverName, iban, bic)
    }
}

@Composable
private fun MakeDepositComposable(
    state: DepositState,
    amount: Amount,
    onMakeDeposit: (Amount, String, String, String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var name by rememberSaveable { mutableStateOf("") }
        var iban by rememberSaveable { mutableStateOf("") }
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
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.amount_chosen),
        )
        Text(
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            color = colorResource(R.color.green),
            text = amount.toString(),
        )
        AnimatedVisibility(visible = state.showFees) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val effectiveAmount = state.effectiveDepositAmount
                val fee = amount - (effectiveAmount ?: Amount.zero(amount.currency))
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
                    text = stringResource(id = R.string.send_deposit_amount_effective),
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    fontSize = 24.sp,
                    color = colorResource(R.color.green),
                    text = effectiveAmount.toString(),
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
        )
        MakeDepositComposable(
            state = state,
            amount = Amount.fromDouble("TESTKUDOS", 42.23)) { _, _, _, _ ->
        }
    }
}
