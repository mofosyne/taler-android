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

package net.taler.wallet.peer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode.EXCHANGE_GENERIC_KYC_REQUIRED
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.compose.QrCodeUriComposable
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.PeerInfoShort
import net.taler.wallet.transactions.TransactionAction.Abort
import net.taler.wallet.transactions.TransactionAction.Retry
import net.taler.wallet.transactions.TransactionAction.Suspend
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionMajorState.Pending
import net.taler.wallet.transactions.TransactionMinorState.Ready
import net.taler.wallet.transactions.TransactionPeerComposable
import net.taler.wallet.transactions.TransactionPeerPushDebit
import net.taler.wallet.transactions.TransactionState

@Composable
fun ColumnScope.TransactionPeerPushDebitComposable(t: TransactionPeerPushDebit) {
    TransactionAmountComposable(
        label = stringResource(id = R.string.transaction_paid),
        amount = t.amountEffective,
        amountType = AmountType.Negative,
    )
    TransactionAmountComposable(
        label = stringResource(id = R.string.transaction_order_total),
        amount = t.amountRaw,
        amountType = AmountType.Neutral,
    )
    val fee = t.amountEffective - t.amountRaw
    if (!fee.isZero()) {
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = fee,
            amountType = AmountType.Negative,
        )
    }
    TransactionInfoComposable(
        label = stringResource(id = R.string.send_peer_purpose),
        info = t.info.summary ?: "",
    )
    if (t.txState == TransactionState(Pending, Ready)) {
        QrCodeUriComposable(
            talerUri = t.talerUri,
            clipBoardLabel = "Push payment",
            buttonText = stringResource(id = R.string.copy),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                text = stringResource(id = R.string.receive_peer_invoice_uri),
            )
        }
    }
}

@Preview
@Composable
fun TransactionPeerPushDebitPreview() {
    val t = TransactionPeerPushDebit(
        transactionId = "transactionId",
        timestamp = Timestamp.fromMillis(System.currentTimeMillis() - 360 * 60 * 1000),
        txState = TransactionState(Pending),
        txActions = listOf(Retry, Suspend, Abort),
        exchangeBaseUrl = "https://exchange.example.org/",
        amountRaw = Amount.fromString("TESTKUDOS", "42.1337"),
        amountEffective = Amount.fromString("TESTKUDOS", "42.23"),
        info = PeerInfoShort(
            expiration = Timestamp.fromMillis(System.currentTimeMillis() + 60 * 60 * 1000),
            summary = "test invoice",
        ),
        talerUri = "https://exchange.example.org/peer/pull/credit",
        error = TalerErrorInfo(code = EXCHANGE_GENERIC_KYC_REQUIRED),
    )
    Surface {
        TransactionPeerComposable(t, true) {}
    }
}
