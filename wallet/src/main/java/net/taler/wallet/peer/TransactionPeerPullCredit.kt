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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.compose.QrCodeUriComposable
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.PeerInfoShort
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionPeerComposable
import net.taler.wallet.transactions.TransactionPeerPullCredit

@Composable
fun ColumnScope.TransactionPeerPullCreditComposable(t: TransactionPeerPullCredit) {
    TransactionAmountComposable(
        label = stringResource(id = R.string.receive_amount),
        amount = t.amountEffective,
        amountType = AmountType.Positive,
    )
    TransactionAmountComposable(
        label = stringResource(id = R.string.amount_chosen),
        amount = t.amountRaw,
        amountType = AmountType.Neutral,
    )
    val fee = t.amountRaw - t.amountEffective
    if (!fee.isZero()) {
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = fee,
            amountType = AmountType.Negative,
        )
    }
    TransactionInfoComposable(
        label = stringResource(id = R.string.withdraw_manual_ready_subject),
        info = t.info.summary ?: "",
    )
    if (t.pending) {
        QrCodeUriComposable(
            talerUri = t.talerUri,
            clipBoardLabel = "Invoice",
            buttonText = stringResource(id = R.string.copy_uri),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.body1,
                text = stringResource(id = R.string.receive_peer_invoice_uri),
            )
        }
    }
}

@Preview
@Composable
fun TransactionPeerPullCreditPreview() {
    val t = TransactionPeerPullCredit(
        transactionId = "transactionId",
        timestamp = Timestamp.fromMillis(System.currentTimeMillis() - 360 * 60 * 1000),
        pending = true,
        exchangeBaseUrl = "https://exchange.example.org/",
        amountRaw = Amount.fromDouble("TESTKUDOS", 42.23),
        amountEffective = Amount.fromDouble("TESTKUDOS", 42.1337),
        info = PeerInfoShort(
            expiration = Timestamp.fromMillis(System.currentTimeMillis() + 60 * 60 * 1000),
            summary = "test invoice",
        ),
        talerUri = "https://exchange.example.org/peer/pull/credit",
    )
    Surface {
        TransactionPeerComposable(t) {}
    }
}
