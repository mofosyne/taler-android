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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                val t = transactionManager.selectedTransaction.observeAsState(null).value
                if (t != null) TransactionPeerComposable(t, devMode.value) {
                    onTransitionButtonClicked(t, it)
                }
            }
        }
    }
}

@Composable
fun TransactionPeerComposable(t: Transaction, devMode: Boolean?, onTransition: (t: TransactionAction) -> Unit) {
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
            style = MaterialTheme.typography.bodyLarge,
        )
        when (t) {
            is TransactionPeerPullCredit -> TransactionPeerPullCreditComposable(t)
            is TransactionPeerPushCredit -> TransactionPeerPushCreditComposable(t)
            is TransactionPeerPullDebit -> TransactionPeerPullDebitComposable(t)
            is TransactionPeerPushDebit -> TransactionPeerPushDebitComposable(t)
            else -> error("unexpected transaction: ${t::class.simpleName}")
        }
        TransitionsComposable(t, onTransition)
        if (devMode == true && t.error != null) {
            ErrorTransactionButton(error = t.error!!)
        }
    }
}

@Composable
fun TransactionAmountComposable(label: String, amount: Amount, amountType: AmountType) {
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = label,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        text = if (amountType == AmountType.Negative) "-$amount" else amount.toString(),
        fontSize = 24.sp,
        color = when (amountType) {
            AmountType.Positive -> colorResource(R.color.green)
            AmountType.Negative -> MaterialTheme.colorScheme.error
            AmountType.Neutral -> Color.Unspecified
        },
    )
}

@Composable
fun TransactionInfoComposable(label: String, info: String) {
    Text(
        modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
        text = label,
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        text = info,
        fontSize = 24.sp,
    )
}
