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

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.QrCodeManager
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.compose.QrCodeUriComposable
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.compose.getQrCodeSize
import org.json.JSONObject

@Composable
fun OutgoingPushResultComposable(state: OutgoingState, onClose: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.h6,
            text = stringResource(id = R.string.send_peer_payment_instruction),
        )
        when (state) {
            OutgoingIntro -> error("Result composable with PullPaymentIntro")
            is OutgoingCreating -> PeerPushCreatingComposable()
            is OutgoingResponse -> PeerPushResponseComposable(state)
            is OutgoingError -> PeerPushErrorComposable(state)
        }
        Button(modifier = Modifier
            .padding(16.dp)
            .align(CenterHorizontally),
            onClick = onClose) {
            Text(text = stringResource(R.string.close))
        }
    }
}

@Composable
private fun ColumnScope.PeerPushCreatingComposable() {
    val qrCodeSize = getQrCodeSize()
    CircularProgressIndicator(
        modifier = Modifier
            .padding(32.dp)
            .size(qrCodeSize)
            .align(CenterHorizontally),
    )
}

@Composable
private fun ColumnScope.PeerPushResponseComposable(state: OutgoingResponse) {
    QrCodeUriComposable(
        talerUri = state.talerUri,
        clipBoardLabel = "Invoice",
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.body1,
            text = stringResource(id = R.string.receive_peer_invoice_uri),
        )
    }
}

@Composable
private fun ColumnScope.PeerPushErrorComposable(state: OutgoingError) {
    Text(
        modifier = Modifier
            .align(CenterHorizontally)
            .padding(16.dp),
        color = colorResource(R.color.red),
        style = MaterialTheme.typography.body1,
        text = state.info.userFacingMsg,
    )
}

@Preview
@Composable
fun PeerPushCreatingPreview() {
    Surface {
        OutgoingPushResultComposable(OutgoingCreating) {}
    }
}

@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PeerPushResponsePreview() {
    TalerSurface {
        val talerUri = "https://example.org/foo/bar/can/be/very/long/url/so/fit/it/on/screen"
        val response = OutgoingResponse(talerUri, QrCodeManager.makeQrCode(talerUri))
        OutgoingPushResultComposable(response) {}
    }
}

@Preview(widthDp = 720, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PeerPushResponseLandscapePreview() {
    TalerSurface {
        val talerUri = "https://example.org/foo/bar/can/be/very/long/url/so/fit/it/on/screen"
        val response = OutgoingResponse(talerUri, QrCodeManager.makeQrCode(talerUri))
        OutgoingPushResultComposable(response) {}
    }
}

@Preview
@Composable
fun PeerPushErrorPreview() {
    Surface {
        val json = JSONObject().apply { put("foo", "bar") }
        val response = OutgoingError(TalerErrorInfo(WALLET_WITHDRAWAL_KYC_REQUIRED, "hint", "message", json))
        OutgoingPushResultComposable(response) {}
    }
}
