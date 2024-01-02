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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.Center
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
import net.taler.wallet.cleanExchange
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfoComposable
import kotlin.random.Random

@Composable
fun OutgoingPullComposable(
    amount: Amount,
    state: OutgoingState,
    onCreateInvoice: (amount: Amount, subject: String, hours: Long, exchange: ExchangeItem) -> Unit,
    onClose: () -> Unit,
) {
    when(state) {
        is OutgoingChecking, is OutgoingCreating, is OutgoingResponse -> PeerCreatingComposable()
        is OutgoingIntro, is OutgoingChecked -> OutgoingPullIntroComposable(
            amount = amount,
            state = state,
            onCreateInvoice = onCreateInvoice,
        )
        is OutgoingError -> PeerErrorComposable(state, onClose)
    }
}

@Composable
fun PeerCreatingComposable() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(32.dp)
                .align(Center),
        )
    }
}

@Composable
fun OutgoingPullIntroComposable(
    amount: Amount,
    state: OutgoingState,
    onCreateInvoice: (amount: Amount, subject: String, hours: Long, exchange: ExchangeItem) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
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

        TransactionAmountComposable(
            label = stringResource(id = R.string.amount_chosen),
            amount = amount,
            amountType = AmountType.Positive,
        )

        if (state is OutgoingChecked) {
            val fee = state.amountRaw - state.amountEffective
            if (!fee.isZero()) TransactionAmountComposable(
                label = stringResource(id = R.string.withdraw_fees),
                amount = fee,
                amountType = AmountType.Negative,
            )
        }

        val exchangeItem = (state as? OutgoingChecked)?.exchangeItem
        TransactionInfoComposable(
            label = stringResource(id = R.string.withdraw_exchange),
            info = if (exchangeItem == null) "" else cleanExchange(exchangeItem.exchangeBaseUrl),
        )

        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            text = stringResource(R.string.send_peer_expiration_period),
            style = MaterialTheme.typography.bodyMedium,
        )

        var option by rememberSaveable { mutableStateOf(DEFAULT_EXPIRY) }
        var hours by rememberSaveable { mutableStateOf(DEFAULT_EXPIRY.hours) }
        ExpirationComposable(
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            option = option,
            hours = hours,
            onOptionChange = { option = it }
        ) { hours = it }

        Button(
            modifier = Modifier.padding(16.dp),
            enabled = subject.isNotBlank() && state is OutgoingChecked,
            onClick = {
                onCreateInvoice(
                    amount,
                    subject,
                    hours,
                    exchangeItem ?: error("clickable without exchange")
                )
            },
        ) {
            Text(text = stringResource(R.string.receive_peer_create_button))
        }
    }
}

@Composable
fun PeerErrorComposable(state: OutgoingError, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = CenterHorizontally,
    ) {
        Text(
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            text = state.info.userFacingMsg,
        )

        Button(
            modifier = Modifier.padding(16.dp),
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(text = stringResource(R.string.close))
        }
    }
}

@Preview
@Composable
fun PeerPullComposableCreatingPreview() {
    Surface {
        OutgoingPullComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = OutgoingCreating,
            onCreateInvoice = { _, _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPullComposableCheckingPreview() {
    Surface {
        OutgoingPullComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = if (Random.nextBoolean()) OutgoingIntro else OutgoingChecking,
            onCreateInvoice = { _, _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPullComposableCheckedPreview() {
    Surface {
        val amountRaw = Amount.fromString("TESTKUDOS", "42.42")
        val amountEffective = Amount.fromString("TESTKUDOS", "42.23")
        val exchangeItem = ExchangeItem("https://example.org", "TESTKUDOS", emptyList())
        OutgoingPullComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = OutgoingChecked(amountRaw, amountEffective, exchangeItem),
            onCreateInvoice = { _, _, _, _ -> },
            onClose = {},
        )
    }
}

@Preview
@Composable
fun PeerPullComposableErrorPreview() {
    Surface {
        val json = mapOf("foo" to JsonPrimitive("bar"))
        val state = OutgoingError(TalerErrorInfo(TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED, "hint", "message", json))
        OutgoingPullComposable(
            amount = Amount.fromString("TESTKUDOS", "42.23"),
            state = state,
            onCreateInvoice = { _, _, _, _ -> },
            onClose = {},
        )
    }
}