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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonPrimitive
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.compose.TalerSurface
import kotlin.random.Random

@Composable
fun OutgoingPushComposable(
    state: OutgoingState,
    amount: Amount,
    onSend: (amount: Amount, summary: String, hours: Long) -> Unit,
    onClose: () -> Unit,
) {
    when(state) {
        is OutgoingChecking, is OutgoingCreating, is OutgoingResponse -> PeerCreatingComposable()
        is OutgoingIntro, is OutgoingChecked -> OutgoingPushIntroComposable(
            amount = amount,
            state = state,
            onSend = onSend,
        )
        is OutgoingError -> PeerErrorComposable(state, onClose)
    }
}

@Composable
fun OutgoingPushIntroComposable(
    state: OutgoingState,
    amount: Amount,
    onSend: (amount: Amount, summary: String, hours: Long) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(vertical = 16.dp),
            text = amount.toString(),
            softWrap = false,
            style = MaterialTheme.typography.titleLarge,
        )

        if (state is OutgoingChecked) {
            val fee = state.amountEffective - state.amountRaw
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(id = R.string.payment_fee, fee.withSpec(amount.spec)),
                softWrap = false,
                color = MaterialTheme.colorScheme.error,
            )
        }

        var subject by rememberSaveable { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(top = 5.dp),
            color = if (subject.isBlank()) MaterialTheme.colorScheme.error else Color.Unspecified,
            text = stringResource(R.string.char_count, subject.length, MAX_LENGTH_SUBJECT),
            textAlign = TextAlign.End,
        )

        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.send_peer_expiration_period),
            style = MaterialTheme.typography.bodyMedium,
        )

        var option by rememberSaveable { mutableStateOf(DEFAULT_EXPIRY) }
        var hours by rememberSaveable { mutableLongStateOf(DEFAULT_EXPIRY.hours) }
        ExpirationComposable(
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            option = option,
            hours = hours,
            onOptionChange = { option = it }
        ) { hours = it }

        Button(
            enabled = state is OutgoingChecked && subject.isNotBlank(),
            onClick = { onSend(amount, subject, hours) },
        ) {
            Text(text = stringResource(R.string.send_peer_create_button))
        }
    }
}

@Preview
@Composable
fun PeerPushComposableCreatingPreview() {
    TalerSurface {
        OutgoingPushComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = OutgoingCreating,
            onSend = { _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPushComposableCheckingPreview() {
    TalerSurface {
        val state = if (Random.nextBoolean()) OutgoingIntro else OutgoingChecking
        OutgoingPushComposable(
            state = state,
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            onSend = { _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPushComposableCheckedPreview() {
    TalerSurface {
        val amountEffective = Amount.fromString("TESTKUDOS", "42.42")
        val amountRaw = Amount.fromString("TESTKUDOS", "42.23")
        val state = OutgoingChecked(amountRaw, amountEffective)
        OutgoingPushComposable(
            state = state,
            amount = amountEffective,
            onSend = { _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPushComposableErrorPreview() {
    TalerSurface {
        val json = mapOf("foo" to JsonPrimitive("bar"))
        val state = OutgoingError(TalerErrorInfo(TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED, "hint", "message", json))
        OutgoingPushComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = state,
            onSend = { _, _, _ -> },
            onClose = {},
        )
    }
}