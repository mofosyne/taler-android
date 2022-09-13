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

package net.taler.wallet.withdraw.manual

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.compose.CopyToClipboardButton
import net.taler.wallet.withdraw.WithdrawStatus

@Composable
fun ScreenBitcoin(
    status: WithdrawStatus.ManualTransferRequiredBitcoin,
    bankAppClick: (() -> Unit)?,
    onCancelClick: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .wrapContentWidth(Alignment.CenterHorizontally)
        .verticalScroll(scrollState)
        .padding(all = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_title),
            style = MaterialTheme.typography.h5,
        )
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_intro),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        BitcoinSegwitAddrs(
            amount = status.amountRaw,
            addr = status.account,
            segwitAddresses = status.segwitAddrs
        )
        if (bankAppClick != null) {
            Button(
                onClick = bankAppClick,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_bank_button))
            }
        }
        if (onCancelClick != null) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(R.color.red)),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(End),
            ) {
                Text(
                    text = stringResource(R.string.withdraw_manual_ready_cancel),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun BitcoinSegwitAddrs(amount: Amount, addr: String, segwitAddresses: List<String>) {
    Column {
        CopyToClipboardButton(
            modifier = Modifier.align(End),
            label = "Bitcoin",
            content = getCopyText(amount, addr, segwitAddresses),
        )
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(0.3f)) {
                Text(
                    text = addr,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Normal,
                    fontSize = 3.em
                )
                Text(
                    text = amount.withCurrency("BTC").toString(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        for (segwitAddress in segwitAddresses) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(0.3f)) {
                    Text(
                        text = segwitAddress,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Normal,
                        fontSize = 3.em,
                    )
                    Text(
                        text = SEGWIT_MIN.toString(),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private val SEGWIT_MIN = Amount("BTC", 0, 294)

private fun getCopyText(amount: Amount, addr: String, segwitAddresses: List<String>): String {
    val sr = segwitAddresses.joinToString(separator = "\n") { s ->
        "\n$s ${SEGWIT_MIN}\n"
    }
    return "$addr ${amount.withCurrency("BTC")}\n$sr"
}

@Preview
@Composable
fun PreviewScreenBitcoin() {
    Surface {
        ScreenBitcoin(WithdrawStatus.ManualTransferRequiredBitcoin(
            exchangeBaseUrl = "bitcoin.ice.bfh.ch",
            uri = Uri.parse("https://taler.net"),
            account = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            segwitAddrs = listOf(
                "bc1qqleages8702xvg9qcyu02yclst24xurdrynvxq",
                "bc1qsleagehks96u7jmqrzcf0fw80ea5g57qm3m84c"
            ),
            subject = "0ZSX8SH0M30KHX8K3Y1DAMVGDQV82XEF9DG1HC4QMQ3QWYT4AF00",
            amountRaw = Amount("BITCOINBTC", 0, 14000000),
            transactionId = "",
        ), {}) {}
    }
}
