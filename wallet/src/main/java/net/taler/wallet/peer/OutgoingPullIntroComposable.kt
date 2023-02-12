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

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.exchanges.ExchangeItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutgoingPullIntroComposable(
    amount: Amount,
    exchangeState: State<ExchangeItem?>,
    onCreateInvoice: (amount: Amount, subject: String, exchange: ExchangeItem) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        var subject by rememberSaveable { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val exchangeItem = exchangeState.value
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .focusRequester(focusRequester),
            singleLine = true,
            value = subject,
            onValueChange = { input ->
                if (input.length <= MAX_LENGTH_SUBJECT)
                    subject = input.replace('\n', ' ')
            },
            isError = subject.isBlank(),
            label = {
                Text(
                    stringResource(R.string.send_peer_purpose),
                    color = if (subject.isBlank()) {
                        MaterialTheme.colorScheme.error
                    } else Color.Unspecified,
                )
            }
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, end = 16.dp),
            color = if (subject.isBlank()) MaterialTheme.colorScheme.error else Color.Unspecified,
            text = stringResource(R.string.char_count, subject.length, MAX_LENGTH_SUBJECT),
            textAlign = TextAlign.End,
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(id = R.string.amount_chosen),
        )
        Text(
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            color = colorResource(R.color.success),
            text = amount.toString(),
        )
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.withdraw_exchange),
        )
        Text(
            modifier = Modifier.padding(16.dp),
            fontSize = 24.sp,
            text = if (exchangeItem == null) "" else cleanExchange(exchangeItem.exchangeBaseUrl),
        )
        Button(
            modifier = Modifier.padding(16.dp),
            enabled = subject.isNotBlank() && exchangeItem != null,
            onClick = {
                onCreateInvoice(amount, subject, exchangeItem ?: error("clickable without exchange"))
            },
        ) {
            Text(text = stringResource(R.string.receive_peer_create_button))
        }
    }
}

@Preview
@Composable
fun PreviewReceiveFundsIntro() {
    Surface {
        @SuppressLint("UnrememberedMutableState")
        val exchangeFlow =
            mutableStateOf(ExchangeItem("https://example.org", "TESTKUDOS", emptyList()))
        OutgoingPullIntroComposable(Amount.fromDouble("TESTKUDOS", 42.23), exchangeFlow) { _, _, _ -> }
    }
}
