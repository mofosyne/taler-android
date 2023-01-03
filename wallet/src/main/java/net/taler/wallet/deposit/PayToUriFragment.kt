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

package net.taler.wallet.deposit

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.wallet.AmountResult
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.TalerSurface

class PayToUriFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val depositManager get() = model.depositManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val uri = arguments?.getString("uri") ?: error("no amount passed")

        val currencies = model.getCurrencies()
        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    if (currencies.isEmpty()) Text(
                        text = stringResource(id = R.string.payment_balance_insufficient),
                        color = colorResource(id = R.color.red),
                    ) else if (depositManager.isSupportedPayToUri(uri)) PayToComposable(
                        currencies = model.getCurrencies(),
                        getAmount = model::createAmount,
                        onAmountChosen = { amount ->
                            val u = Uri.parse(uri)
                            val bundle = bundleOf(
                                "amount" to amount.toJSONString(),
                                "receiverName" to u.getQueryParameters("receiver-name")[0],
                                "IBAN" to u.pathSegments.last(),
                            )
                            findNavController().navigate(
                                R.id.action_nav_payto_uri_to_nav_deposit, bundle)
                        },
                    ) else Text(
                        text = stringResource(id = R.string.uri_invalid),
                        color = colorResource(id = R.color.red),
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.send_deposit_title)
    }

}

@Composable
private fun PayToComposable(
    currencies: List<String>,
    getAmount: (String, String) -> AmountResult,
    onAmountChosen: (Amount) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        var amountText by rememberSaveable { mutableStateOf("") }
        var amountError by rememberSaveable { mutableStateOf("") }
        var currency by rememberSaveable { mutableStateOf(currencies[0]) }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester),
            value = amountText,
            onValueChange = { input ->
                amountError = ""
                amountText = input
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = amountError.isNotBlank(),
            label = {
                if (amountError.isBlank()) {
                    Text(stringResource(R.string.send_amount))
                } else {
                    Text(amountError, color = colorResource(R.color.red))
                }
            }
        )
        CurrencyDropdown(
            currencies = currencies,
            onCurrencyChanged = { c -> currency = c },
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        val focusManager = LocalFocusManager.current
        val errorStrInvalidAmount = stringResource(id = R.string.receive_amount_invalid)
        val errorStrInsufficientBalance = stringResource(id = R.string.payment_balance_insufficient)
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = amountText.isNotBlank(),
            onClick = {
                when (val amountResult = getAmount(amountText, currency)) {
                    is AmountResult.Success -> {
                        focusManager.clearFocus()
                        onAmountChosen(amountResult.amount)
                    }
                    is AmountResult.InvalidAmount -> amountError = errorStrInvalidAmount
                    is AmountResult.InsufficientBalance -> amountError = errorStrInsufficientBalance
                }
            },
        ) {
            Text(text = stringResource(R.string.send_deposit_check_fees_button))
        }
    }
}

@Composable
fun CurrencyDropdown(
    currencies: List<String>,
    onCurrencyChanged: (String) -> Unit,
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center),
    ) {
        OutlinedTextField(
            modifier = Modifier
                .clickable(onClick = { expanded = true }),
            value = currencies[selectedIndex],
            onValueChange = { },
            readOnly = true,
            enabled = false,
            textStyle = LocalTextStyle.current.copy( // show text as if not disabled
                color = TextFieldDefaults.outlinedTextFieldColors().textColor(
                    enabled = true,
                ).value
            ),
            singleLine = true,
            label = {
                Text(stringResource(R.string.currency))
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier,
        ) {
            currencies.forEachIndexed { index, s ->
                DropdownMenuItem(onClick = {
                    selectedIndex = index
                    onCurrencyChanged(currencies[index])
                    expanded = false
                }) {
                    Text(text = s)
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewPayToComposable() {
    Surface {
        PayToComposable(
            currencies = listOf("KUDOS", "TESTKUDOS", "BTCBITCOIN"),
            getAmount = { _, _ -> AmountResult.InvalidAmount },
            onAmountChosen = {},
        )
    }
}
