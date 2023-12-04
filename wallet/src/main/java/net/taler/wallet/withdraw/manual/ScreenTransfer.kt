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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.CURRENCY_BTC
import net.taler.wallet.R
import net.taler.wallet.compose.copyToClipBoard
import net.taler.wallet.currency.CurrencySpecification
import net.taler.wallet.transactions.WithdrawalExchangeAccountDetails
import net.taler.wallet.withdraw.TransferData
import net.taler.wallet.withdraw.WithdrawStatus

@Composable
fun ScreenTransfer(
    status: WithdrawStatus.ManualTransferRequired,
    bankAppClick: ((transfer: TransferData) -> Unit)?,
    onCancelClick: (() -> Unit)?,
) {
    // TODO: show some placeholder
    if (status.withdrawalTransfers.isEmpty()) return

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.withdraw_manual_ready_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        val defaultTransfer = status.withdrawalTransfers[0]
        var selectedTransfer by remember { mutableStateOf(defaultTransfer) }

        if (status.withdrawalTransfers.size > 1) {
            TransferAccountChooser(
                accounts = status.withdrawalTransfers.map { it.withdrawalAccount },
                selectedAccount = selectedTransfer.withdrawalAccount,
                onSelectAccount = { account ->
                    status.withdrawalTransfers.find {
                        it.withdrawalAccount.paytoUri == account.paytoUri
                    }?.let { selectedTransfer = it }
                }
            )
        }

        when (val transfer = selectedTransfer) {
            is TransferData.IBAN -> TransferIBAN(
                transfer = transfer,
                exchangeBaseUrl = status.exchangeBaseUrl,
                transactionAmountRaw = status.transactionAmountRaw,
                transactionAmountEffective = status.transactionAmountEffective,
            )
            is TransferData.Bitcoin -> TransferBitcoin(
                transfer = transfer,
                transactionAmountRaw = status.transactionAmountRaw,
                transactionAmountEffective = status.transactionAmountEffective,
            )
        }

        if (bankAppClick != null) {
            Button(
                onClick = { bankAppClick(selectedTransfer) },
                modifier = Modifier
                    .padding(top = 16.dp)
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_bank_button))
            }
        }

        if (onCancelClick != null) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.withdraw_manual_ready_cancel),
                    color = MaterialTheme.colorScheme.onError,
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    content: String,
    copy: Boolean = true,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 6.dp, end = 6.dp),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
        )

        Row(
            modifier = Modifier.padding(top = 8.dp, start = 6.dp, end = 6.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = if (copy) Modifier.weight(1f) else Modifier,
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = if (copy) FontFamily.Monospace else FontFamily.Default,
                textAlign = TextAlign.Center,
            )

            if (copy) {
                IconButton(
                    modifier = Modifier.padding(start = 8.dp),
                    onClick = { copyToClipBoard(context, label, content) },
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = stringResource(R.string.copy),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ScreenTransferPreview() {
    Surface {
        ScreenTransfer(
            status = WithdrawStatus.ManualTransferRequired(
                transactionId = "",
                transactionAmountRaw = Amount.fromJSONString("KUDOS:10"),
                transactionAmountEffective = Amount.fromJSONString("KUDOS:9.5"),
                exchangeBaseUrl = "test.exchange.taler.net",
                withdrawalTransfers = listOf(
                    TransferData.IBAN(
                        iban = "ASDQWEASDZXCASDQWE",
                        subject = "Taler Withdrawal P2T19EXRBY4B145JRNZ8CQTD7TCS03JE9VZRCEVKVWCP930P56WG",
                        amountRaw = Amount("KUDOS", 10, 0),
                        amountEffective = Amount("KUDOS", 9, 5),
                        withdrawalAccount = WithdrawalExchangeAccountDetails(
                            paytoUri = "https://taler.net/kudos",
                            transferAmount = Amount("KUDOS", 10, 0),
                            currencySpecification = CurrencySpecification(
                                "KUDOS",
                                numFractionalInputDigits = 2,
                                numFractionalNormalDigits = 2,
                                numFractionalTrailingZeroDigits = 2,
                                altUnitNames = emptyMap(),
                            ),
                        ),
                    ),
                    TransferData.Bitcoin(
                        account = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
                        segwitAddresses = listOf(
                            "bc1qqleages8702xvg9qcyu02yclst24xurdrynvxq",
                            "bc1qsleagehks96u7jmqrzcf0fw80ea5g57qm3m84c"
                        ),
                        subject = "0ZSX8SH0M30KHX8K3Y1DAMVGDQV82XEF9DG1HC4QMQ3QWYT4AF00",
                        amountRaw = Amount(CURRENCY_BTC, 0, 14000000),
                        amountEffective = Amount(CURRENCY_BTC, 0, 14000000),
                        withdrawalAccount = WithdrawalExchangeAccountDetails(
                            paytoUri = "https://taler.net/btc",
                            transferAmount = Amount("BTC", 0, 14000000),
                            currencySpecification = CurrencySpecification(
                                "Bitcoin",
                                numFractionalInputDigits = 2,
                                numFractionalNormalDigits = 2,
                                numFractionalTrailingZeroDigits = 2,
                                altUnitNames = emptyMap(),
                            ),
                        ),
                    )
                ),
            ),
            bankAppClick = {},
            onCancelClick = {},
        )
    }
}