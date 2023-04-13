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

package net.taler.wallet.withdraw

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.cleanExchange
import net.taler.wallet.transactions.ActionButton
import net.taler.wallet.transactions.ActionListener
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.DeleteTransactionComposable
import net.taler.wallet.transactions.ErrorTransactionButton
import net.taler.wallet.transactions.ExtendedStatus
import net.taler.wallet.transactions.Transaction
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionWithdrawal
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer

@Composable
fun TransactionWithdrawalComposable(
    t: TransactionWithdrawal,
    devMode: Boolean,
    actionListener: ActionListener,
    onDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        Text(
            modifier = Modifier.padding(16.dp),
            text = t.timestamp.ms.toAbsoluteTime(context).toString(),
            style = MaterialTheme.typography.bodyLarge,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_total),
            amount = t.amountEffective,
            amountType = AmountType.Positive,
        )
        ActionButton(tx = t, listener = actionListener)
        TransactionAmountComposable(
            label = stringResource(id = R.string.amount_chosen),
            amount = t.amountRaw,
            amountType = AmountType.Neutral,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = t.amountRaw - t.amountEffective,
            amountType = AmountType.Negative,
        )
        TransactionInfoComposable(
            label = stringResource(id = R.string.withdraw_exchange),
            info = cleanExchange(t.exchangeBaseUrl),
        )
        if (t.extendedStatus == ExtendedStatus.Pending) {
            Button(
                modifier = Modifier.padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = onDelete,
            ) {
                Row(verticalAlignment = CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cancel),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onError,
                    )
                }
            }
        } else {
            DeleteTransactionComposable(onDelete)
        }
        if (devMode && t.error != null) {
            ErrorTransactionButton(error = t.error)
        }
    }
}

@Preview
@Composable
fun TransactionWithdrawalComposablePreview() {
    val t = TransactionWithdrawal(
        transactionId = "transactionId",
        timestamp = Timestamp.fromMillis(System.currentTimeMillis() - 360 * 60 * 1000),
        extendedStatus = ExtendedStatus.Pending,
        exchangeBaseUrl = "https://exchange.demo.taler.net/",
        withdrawalDetails = ManualTransfer(exchangePaytoUris = emptyList()),
        amountRaw = Amount.fromDouble("TESTKUDOS", 42.23),
        amountEffective = Amount.fromDouble("TESTKUDOS", 42.1337),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    val listener = object : ActionListener {
        override fun onActionButtonClicked(tx: Transaction, type: ActionListener.Type) {
        }
    }
    Surface {
        TransactionWithdrawalComposable(t, true, listener) {}
    }
}
