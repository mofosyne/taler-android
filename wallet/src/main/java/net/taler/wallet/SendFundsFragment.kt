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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.common.CurrencySpecification
import net.taler.wallet.compose.AmountInputField
import net.taler.wallet.compose.DEFAULT_INPUT_DECIMALS
import net.taler.wallet.compose.TalerSurface

class SendFundsFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val balanceManager get() = model.balanceManager
    private val peerManager get() = model.peerManager
    private val scopeInfo get() = model.transactionManager.selectedScope ?: error("No scope selected")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            TalerSurface {
                SendFundsIntro(
                    currency = scopeInfo.currency,
                    spec = balanceManager.getSpecForScopeInfo(scopeInfo),
                    hasSufficientBalance = model::hasSufficientBalance,
                    onDeposit = this@SendFundsFragment::onDeposit,
                    onPeerPush = this@SendFundsFragment::onPeerPush,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(getString(R.string.transactions_send_funds_title, scopeInfo.currency))
    }

    private fun onDeposit(amount: Amount) {
        val bundle = bundleOf("amount" to amount.toJSONString())
        findNavController().navigate(R.id.action_sendFunds_to_nav_deposit, bundle)
    }

    private fun onPeerPush(amount: Amount) {
        val bundle = bundleOf("amount" to amount.toJSONString())
        peerManager.checkPeerPushDebit(amount)
        findNavController().navigate(R.id.action_sendFunds_to_nav_peer_push, bundle)
    }
}

@Composable
private fun SendFundsIntro(
    currency: String,
    spec: CurrencySpecification?,
    hasSufficientBalance: (Amount) -> Boolean,
    onDeposit: (Amount) -> Unit,
    onPeerPush: (Amount) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        var text by rememberSaveable { mutableStateOf("0") }
        var isError by rememberSaveable { mutableStateOf(false) }
        var insufficientBalance by rememberSaveable { mutableStateOf(false) }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp),
        ) {
            AmountInputField(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                value = text,
                onValueChange = { input ->
                    isError = false
                    insufficientBalance = false
                    text = input
                },
                label = { Text(stringResource(R.string.send_amount)) },
                supportingText = {
                    if (isError) Text(stringResource(R.string.receive_amount_invalid))
                    else if (insufficientBalance) {
                        Text(stringResource(R.string.payment_balance_insufficient))
                    }
                },
                isError = isError || insufficientBalance,
                numberOfDecimals = spec?.numFractionalInputDigits ?: DEFAULT_INPUT_DECIMALS,
            )
            Text(
                modifier = Modifier,
                text = spec?.symbol ?: currency,
                softWrap = false,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.send_intro),
            style = MaterialTheme.typography.titleLarge,
        )
        Row(modifier = Modifier.padding(16.dp)) {
            fun onClickButton(block: (Amount) -> Unit) {
                val amount = getAmount(currency, text)
                if (amount == null || amount.isZero()) isError = true
                else if (!hasSufficientBalance(amount)) insufficientBalance = true
                else block(amount)
            }
            Button(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .height(IntrinsicSize.Max)
                    .weight(1f),
                onClick = {
                    onClickButton { amount -> onDeposit(amount) }
                }) {
                Text(text = if (currency == CURRENCY_BTC) {
                    stringResource(R.string.send_deposit_bitcoin)
                } else {
                    stringResource(R.string.send_deposit)
                })
            }
            Button(
                modifier = Modifier
                    .height(IntrinsicSize.Max)
                    .weight(1f),
                onClick = {
                    onClickButton { amount -> onPeerPush(amount) }
                },
            ) {
                Text(text = if (currency == CURRENCY_BTC) {
                    stringResource(R.string.send_peer_bitcoin)
                } else {
                    stringResource(R.string.send_peer)
                })
            }
        }
    }
}

@Preview
@Composable
fun PreviewSendFundsIntro() {
    Surface {
        SendFundsIntro("TESTKUDOS", null, { true }, {}) {}
    }
}
