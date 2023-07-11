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

package net.taler.wallet.transactions

import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.taler.wallet.R
import net.taler.wallet.transactions.TransactionAction.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransitionsComposable(
    t: Transaction,
    devMode: Boolean,
    onTransition: (t: TransactionAction) -> Unit,
) {
    FlowRow(horizontalArrangement = Center) {
        t.txActions.forEach {
            if (it in arrayOf(Resume, Suspend)) {
                if (devMode) TransitionComposable(it, onTransition)
            } else {
                TransitionComposable(it, onTransition)
            }
        }
    }
}

@Composable
fun TransitionComposable(t: TransactionAction, onClick: (t: TransactionAction) -> Unit) {
    Button(
        modifier = Modifier.padding(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = when (t) {
                Delete -> MaterialTheme.colorScheme.error
                Retry -> MaterialTheme.colorScheme.primary
                Abort -> MaterialTheme.colorScheme.error
                Fail -> MaterialTheme.colorScheme.error
                Resume -> MaterialTheme.colorScheme.primary
                Suspend -> MaterialTheme.colorScheme.primary
            }
        ),
        onClick = { onClick(t) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = when (t) {
                    Delete -> painterResource(id = R.drawable.ic_delete)
                    Retry -> painterResource(id = R.drawable.ic_retry)
                    Abort -> painterResource(id = R.drawable.ic_cancel)
                    Fail -> painterResource(id = R.drawable.ic_fail)
                    Resume -> painterResource(id = R.drawable.ic_resume)
                    Suspend -> painterResource(id = R.drawable.ic_suspend)
                },
                contentDescription = null,
                tint = when (t) {
                    Delete -> MaterialTheme.colorScheme.onError
                    Retry -> MaterialTheme.colorScheme.onPrimary
                    Abort -> MaterialTheme.colorScheme.onError
                    Fail -> MaterialTheme.colorScheme.onError
                    Resume -> MaterialTheme.colorScheme.onPrimary
                    Suspend -> MaterialTheme.colorScheme.onPrimary
                },
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = when (t) {
                    Delete -> stringResource(R.string.transactions_delete)
                    Retry -> stringResource(R.string.transactions_retry)
                    Abort -> stringResource(R.string.transactions_abort)
                    Fail -> stringResource(R.string.transactions_fail)
                    Resume -> stringResource(R.string.transactions_resume)
                    Suspend -> stringResource(R.string.transactions_suspend)
                },
                color = when (t) {
                    Delete -> MaterialTheme.colorScheme.onError
                    Retry -> MaterialTheme.colorScheme.onPrimary
                    Abort -> MaterialTheme.colorScheme.onError
                    Fail -> MaterialTheme.colorScheme.onError
                    Resume -> MaterialTheme.colorScheme.onPrimary
                    Suspend -> MaterialTheme.colorScheme.onPrimary
                },
            )
        }
    }
}
