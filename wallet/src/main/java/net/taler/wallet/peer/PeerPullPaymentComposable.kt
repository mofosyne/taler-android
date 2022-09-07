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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import net.taler.wallet.backend.TalerErrorInfo

@Composable
fun PeerPullPaymentComposable(
    state: State<PeerIncomingState>,
    onAccept: (PeerIncomingTerms) -> Unit,
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
            text = stringResource(id = R.string.pay_peer_intro))
        when (val s = state.value) {
            PeerIncomingChecking -> PeerPullCheckingComposable()
            is PeerIncomingTerms -> PeerPullTermsComposable(s, onAccept)
            is PeerIncomingAccepting -> PeerPullTermsComposable(s, onAccept)
            PeerIncomingAccepted -> {
                // we navigate away, don't show anything
            }
            is PeerIncomingError -> PeerPullErrorComposable(s)
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
    terms: PeerIncomingTerms,
    onAccept: (PeerIncomingTerms) -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(16.dp)
            .align(CenterHorizontally),
        text = terms.contractTerms.summary,
        style = MaterialTheme.typography.h5,
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
                    style = MaterialTheme.typography.body1,
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = terms.contractTerms.amount.toString(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold,
                )
            }
            val fee =
                Amount.zero(terms.amount.currency) // terms.amount - terms.contractTerms.amount
            if (!fee.isZero()) {
                Text(
                    modifier = Modifier.align(End),
                    text = stringResource(id = R.string.payment_fee, fee),
                    style = MaterialTheme.typography.body1,
                )
            }
            if (terms is PeerIncomingAccepting) {
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
                        backgroundColor = colorResource(R.color.green),
                        contentColor = Color.White,
                    ),
                    onClick = { onAccept(terms) },
                ) {
                    Text(
                        text = stringResource(id = R.string.payment_button_confirm),
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.PeerPullErrorComposable(s: PeerIncomingError) {
    Text(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(horizontal = 32.dp),
        text = s.info.userFacingMsg,
        style = MaterialTheme.typography.h5,
        color = colorResource(id = R.color.red),
    )
}

@Preview
@Composable
fun PeerPullCheckingPreview() {
    Surface {
        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(PeerIncomingChecking)
        PeerPullPaymentComposable(s) {}
    }
}

@Preview
@Composable
fun PeerPullTermsPreview() {
    Surface {
        val terms = PeerIncomingTerms(
            amount = Amount.fromDouble("TESTKUDOS", 42.23),
            contractTerms = PeerContractTerms(
                summary = "This is a long test summary that can be more than one line long for sure",
                amount = Amount.fromDouble("TESTKUDOS", 23.42),
            ),
            id = "ID123",
        )

        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(terms)
        PeerPullPaymentComposable(s) {}
    }
}

@Preview
@Composable
fun PeerPullAcceptingPreview() {
    Surface {
        val terms = PeerIncomingTerms(
            amount = Amount.fromDouble("TESTKUDOS", 42.23),
            contractTerms = PeerContractTerms(
                summary = "This is a long test summary that can be more than one line long for sure",
                amount = Amount.fromDouble("TESTKUDOS", 23.42),
            ),
            id = "ID123",
        )

        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(PeerIncomingAccepting(terms))
        PeerPullPaymentComposable(s) {}
    }
}

@Preview
@Composable
fun PeerPullPayErrorPreview() {
    Surface {
        @SuppressLint("UnrememberedMutableState")
        val s = mutableStateOf(PeerIncomingError(TalerErrorInfo(42, "hint", "msg")))
        PeerPullPaymentComposable(s) {}
    }
}
