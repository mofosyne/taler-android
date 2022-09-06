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

package net.taler.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType.Companion.Decimal
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.Amount
import net.taler.wallet.exchanges.ExchangeItem

class ReceiveFundsFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val exchangeManager get() = model.exchangeManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            MdcTheme {
                Surface {
                    ReceiveFundsIntro(
                        model.transactionManager.selectedCurrency ?: error("No currency selected"),
                        this@ReceiveFundsFragment::onManualWithdraw,
                        this@ReceiveFundsFragment::onPeerPull,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.transactions_receive_funds)
    }

    private fun onManualWithdraw(amount: Amount) {
        // TODO give some UI feedback while we wait for exchanges to load (quick enough for now)
        lifecycleScope.launchWhenResumed {
            // we need to set the exchange first, we want to withdraw from
            exchangeManager.findExchangeForCurrency(amount.currency).collect { exchange ->
                onExchangeRetrieved(exchange, amount)
            }
        }
    }

    private fun onExchangeRetrieved(exchange: ExchangeItem?, amount: Amount) {
        if (exchange == null) {
            Toast.makeText(requireContext(), "No exchange available", LENGTH_LONG).show()
            return
        }
        exchangeManager.withdrawalExchange = exchange
        // now that we have the exchange, we can navigate
        val bundle = bundleOf("amount" to amount.toJSONString())
        findNavController().navigate(
            R.id.action_receiveFunds_to_nav_exchange_manual_withdrawal, bundle)
    }

    private fun onPeerPull(amount: Amount) {
        val bundle = bundleOf("amount" to amount.toJSONString())
        findNavController().navigate(R.id.action_receiveFunds_to_nav_peer_pull, bundle)
    }
}

@Composable
private fun ReceiveFundsIntro(
    currency: String,
    onManualWithdraw: (Amount) -> Unit,
    onPeerPull: (Amount) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        var text by rememberSaveable { mutableStateOf("") }
        var isError by rememberSaveable { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                value = text,
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = Decimal),
                onValueChange = { input ->
                    isError = false
                    text = input.filter { it.isDigit() || it == '.' }
                },
                isError = isError,
                label = {
                    if (isError) {
                        Text(
                            stringResource(R.string.receive_amount_invalid),
                            color = Color.Red,
                        )
                    } else {
                        Text(stringResource(R.string.receive_amount))
                    }
                }
            )
            Text(
                modifier = Modifier,
                text = currency,
                softWrap = false,
                style = MaterialTheme.typography.h6,
            )
        }
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.receive_intro),
            style = MaterialTheme.typography.h6,
        )
        Row(modifier = Modifier.padding(16.dp)) {
            Button(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f),
                onClick = {
                    val amount = getAmount(currency, text)
                    if (amount == null) isError = true
                    else onManualWithdraw(amount)
                }) {
                Text(text = stringResource(R.string.receive_withdraw))
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    val amount = getAmount(currency, text)
                    if (amount == null) isError = true
                    else onPeerPull(amount)
                },
            ) {
                Text(text = stringResource(R.string.receive_peer))
            }
        }
    }
}

@Preview
@Composable
fun PreviewReceiveFundsIntro() {
    Surface {
        ReceiveFundsIntro("TESTKUDOS", {}) {}
    }
}
