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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.compose.CopyToClipboardButton
import net.taler.wallet.withdraw.TransferData
import net.taler.wallet.withdraw.WithdrawalAmountTransfer

@Composable
fun TransferBitcoin(
    transfer: TransferData.Bitcoin,
    transactionAmountRaw: Amount,
    transactionAmountEffective: Amount,
) {
    Column(modifier = Modifier
        .wrapContentWidth(Alignment.CenterHorizontally)
        .padding(all = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_intro),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        BitcoinSegwitAddresses(
            amount = transfer.amountRaw,
            address = transfer.account,
            segwitAddresses = transfer.segwitAddresses,
        )

        WithdrawalAmountTransfer(
            amountRaw = transactionAmountRaw,
            amountEffective = transactionAmountEffective,
            conversionAmountRaw = transfer.amountRaw,
        )
    }
}

@Composable
fun BitcoinSegwitAddresses(amount: Amount, address: String, segwitAddresses: List<String>) {
    Column {
        CopyToClipboardButton(
            modifier = Modifier.align(End),
            label = "Bitcoin",
            content = getCopyText(amount, address, segwitAddresses),
        )
        Row(modifier = Modifier.padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(0.3f)) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    fontSize = 3.em
                )
                Text(
                    text = amount.withCurrency("BTC").toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        for (segwitAddress in segwitAddresses) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(0.3f)) {
                    Text(
                        text = segwitAddress,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Normal,
                        fontSize = 3.em,
                    )
                    Text(
                        text = SEGWIT_MIN.toString(),
                        style = MaterialTheme.typography.bodyLarge,
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