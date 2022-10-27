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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R

@Composable
fun OutgoingPushIntroComposable(
    amount: Amount,
    onSend: (amount: Amount, summary: String) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(16.dp),
        ) {
            Text(
                modifier = Modifier,
                text = amount.toString(),
                softWrap = false,
                style = MaterialTheme.typography.h6,
            )
        }

        var subject by rememberSaveable { mutableStateOf("") }
        OutlinedTextField(
            modifier = Modifier.padding(horizontal = 16.dp),
            value = subject,
            onValueChange = { input ->
                subject = input
            },
            isError = subject.isBlank(),
            label = {
                Text(
                    stringResource(R.string.withdraw_manual_ready_subject),
                    color = if (subject.isBlank()) {
                        colorResource(R.color.red)
                    } else Color.Unspecified,
                )
            }
        )
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.send_peer_warning),
        )
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = subject.isNotBlank(),
            onClick = {
                onSend(amount, subject)
            },
        ) {
            Text(text = stringResource(R.string.send_peer_create_button))
        }
    }
}

@Preview
@Composable
fun PeerPushIntroComposablePreview() {
    Surface {
        OutgoingPushIntroComposable(Amount.fromDouble("TESTKUDOS", 42.23)) { _, _ -> }
    }
}
