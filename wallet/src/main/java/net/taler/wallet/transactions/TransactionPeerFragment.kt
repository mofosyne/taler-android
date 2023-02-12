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

package net.taler.wallet.transactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.peer.TransactionPeerPullCreditComposable
import net.taler.wallet.peer.TransactionPeerPullDebitComposable
import net.taler.wallet.peer.TransactionPeerPushCreditComposable
import net.taler.wallet.peer.TransactionPeerPushDebitComposable

class TransactionPeerFragment : TransactionDetailFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            TalerSurface {
                val t = transaction ?: error("No transaction")
                TransactionPeerComposable(t) {
                    onDeleteButtonClicked(t)
                }
            }
        }
    }
}

@Composable
fun TransactionPeerComposable(t: Transaction, onDelete: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        val context = LocalContext.current
        Text(
            modifier = Modifier.padding(16.dp),
            text = t.timestamp.ms.toAbsoluteTime(context).toString(),
            style = MaterialTheme.typography.labelLarge,
        )
        when (t) {
            is TransactionPeerPullCredit -> TransactionPeerPullCreditComposable(t)
            is TransactionPeerPushCredit -> TransactionPeerPushCreditComposable(t)
            is TransactionPeerPullDebit -> TransactionPeerPullDebitComposable(t)
            is TransactionPeerPushDebit -> TransactionPeerPushDebitComposable(t)
            else -> error("unexpected transaction: ${t::class.simpleName}")
        }
        DeleteTransactionComposable(onDelete)
    }
}

@Composable
fun TransactionAmountComposable(label: String, amount: Amount, amountType: AmountType) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        shape = ShapeDefaults.Medium,
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = if (amountType == AmountType.Negative) "-$amount" else amount.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = when (amountType) {
                    AmountType.Positive -> colorResource(R.color.success)
                    AmountType.Negative -> MaterialTheme.colorScheme.error
                    AmountType.Neutral -> Color.Unspecified
                },
            )
        }
    }
}

@Composable
fun TransactionInfoComposable(label: String, info: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        shape = ShapeDefaults.Medium,
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(all = 16.dp)) {
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = label,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = info,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}
