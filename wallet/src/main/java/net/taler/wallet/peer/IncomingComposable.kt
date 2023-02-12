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
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED
import net.taler.wallet.backend.TalerErrorInfo

data class IncomingData(
    @StringRes val intro: Int,
    @StringRes val button: Int,
)

val incomingPush = IncomingData(
    intro = R.string.receive_peer_payment_intro,
    button = R.string.receive_peer_payment_title,
)

val incomingPull = IncomingData(
    intro = R.string.pay_peer_intro,
    button = R.string.payment_button_confirm,
)

@Composable
fun IncomingComposable(
    state: State<IncomingState>,
    data: IncomingData,
    onAccept: (IncomingTerms) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        Text(
            modifier = Modifier
                .padding(16.dp)
                .align(CenterHorizontally),
            text = stringResource(id = data.intro),
        )
        when (val s = state.value) {
            IncomingChecking -> PeerPullCheckingComposable()
            is IncomingTerms -> PeerPullTermsComposable(s, onAccept, data)
            is IncomingAccepting -> PeerPullTermsComposable(s, onAccept, data)
            IncomingAccepted -> {
                // we navigate away, don't show anything
            }
            is IncomingError -> PeerPullErrorComposable(s)
        }
    }
}

@Composable
fun ColumnScope.PeerPullCheckingComposable() {
    CircularProgressIndicator(
        modifier = Modifier
            .align(CenterHorizontally)
            .fillMaxSize(0.75f),
    )
}

@Composable
fun ColumnScope.PeerPullTermsComposable(
    terms: IncomingTerms,
    onAccept: (IncomingTerms) -> Unit,
    data: IncomingData,
) {
    Text(
        modifier = Modifier
            .padding(16.dp)
            .align(CenterHorizontally),
        text = terms.contractTerms.summary,
        style = MaterialTheme.typography.headlineSmall,
    )
    Spacer(modifier = Modifier.weight(1f))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.align(End),
            ) {
                Text(
                    text = stringResource(id = R.string.payment_label_amount_total),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = terms.contractTerms.amount.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            val fee =
                Amount.zero(terms.amount.currency) // terms.amount - terms.contractTerms.amount
            if (!fee.isZero()) {
                Text(
                    modifier = Modifier.align(End),
                    text = stringResource(id = R.string.payment_fee, fee),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            if (terms is IncomingAccepting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 64.dp)
                        .align(End),
                )
            } else {
                Button(
                    modifier = Modifier
                        .align(End)
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.success),
                        contentColor = Color.White,
                    ),
                    onClick = { onAccept(terms) },
                ) {
                    Text(
                        text = stringResource(id = data.button),
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PeerPullErrorComposable(s: IncomingError) {
    Text(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(horizontal = 32.dp),
        text = s.info.userFacingMsg,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview
@Composable
fun PeerPullCheckingPreview() {
    Surface {
        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(IncomingChecking)
        IncomingComposable(s, incomingPush) {}
    }
}

@Preview
@Composable
fun PeerPullTermsPreview() {
    Surface {
        val terms = IncomingTerms(
            amount = Amount.fromDouble("TESTKUDOS", 42.23),
            contractTerms = PeerContractTerms(
                summary = "This is a long test summary that can be more than one line long for sure",
                amount = Amount.fromDouble("TESTKUDOS", 23.42),
            ),
            id = "ID123",
        )

        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(terms)
        IncomingComposable(s, incomingPush) {}
    }
}

@Preview
@Composable
fun PeerPullAcceptingPreview() {
    Surface {
        val terms = IncomingTerms(
            amount = Amount.fromDouble("TESTKUDOS", 42.23),
            contractTerms = PeerContractTerms(
                summary = "This is a long test summary that can be more than one line long for sure",
                amount = Amount.fromDouble("TESTKUDOS", 23.42),
            ),
            id = "ID123",
        )

        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(IncomingAccepting(terms))
        IncomingComposable(s, incomingPush) {}
    }
}

@Preview
@Composable
fun PeerPullPayErrorPreview() {
    Surface {
        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(IncomingError(TalerErrorInfo(WALLET_WITHDRAWAL_KYC_REQUIRED, "hint", "msg")))
        IncomingComposable(s, incomingPush) {}
    }
}
