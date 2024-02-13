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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.compose.CopyToClipboardButton
import net.taler.wallet.withdraw.TransferData

@Composable
fun TransferBitcoin(
    transfer: TransferData.Bitcoin,
    transactionAmountRaw: Amount,
    transactionAmountEffective: Amount,
) {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        horizontalAlignment = CenterHorizontally,
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

        transfer.withdrawalAccount.transferAmount?.let { amount ->
            WithdrawalAmountTransfer(
                amountRaw = transactionAmountRaw,
                amountEffective = transactionAmountEffective,
                conversionAmountRaw = amount.withSpec(
                    transfer.withdrawalAccount.currencySpecification,
                ),
            )
        }
    }
}

@Composable
fun BitcoinSegwitAddresses(amount: Amount, address: String, segwitAddresses: List<String>) {
    Column {
        val allSegwitAddresses = listOf(address) + segwitAddresses
        for (segwitAddress in allSegwitAddresses) {
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(0.3f)) {
                    Text(
                        text = segwitAddress,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = if (segwitAddress == address)
                            amount.withCurrency("BTC").toString()
                        else SEGWIT_MIN.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        CopyToClipboardButton(
            modifier = Modifier
                .padding(top = 16.dp, start = 6.dp, end = 6.dp)
                .align(CenterHorizontally),
            label = "Bitcoin",
            content = getCopyText(amount, address, segwitAddresses),
        )
    }
}

private val SEGWIT_MIN = Amount("BTC", 0, 294)

private fun getCopyText(amount: Amount, addr: String, segwitAddresses: List<String>): String {
    val sr = segwitAddresses.joinToString(separator = "\n") { s ->
        "\n$s ${SEGWIT_MIN}\n"
    }
    return "$addr ${amount.withCurrency("BTC")}\n$sr"
}