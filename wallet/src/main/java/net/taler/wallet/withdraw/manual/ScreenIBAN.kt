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

package net.taler.wallet.withdraw.manual

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.compose.copyToClipBoard
import net.taler.wallet.withdraw.WithdrawStatus

@Composable
fun ScreenIBAN(
    status: WithdrawStatus.ManualTransferRequiredIBAN,
    bankAppClick: (() -> Unit)?,
    onCancelClick: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(all = 16.dp)
        .wrapContentWidth(Alignment.CenterHorizontally)
        .verticalScroll(scrollState)
    ) {
        Text(
            text = stringResource(R.string.withdraw_manual_ready_title),
            style = MaterialTheme.typography.h5,
        )
        Text(
            text = stringResource(R.string.withdraw_manual_ready_intro,
                status.amountRaw.toString()),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.withdraw_manual_ready_details_intro),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        DetailRow(stringResource(R.string.withdraw_manual_ready_iban), status.iban)
        DetailRow(stringResource(R.string.withdraw_manual_ready_subject), status.subject)
        DetailRow(stringResource(R.string.amount_chosen), status.amountRaw.toString())
        DetailRow(stringResource(R.string.withdraw_exchange), status.exchangeBaseUrl, false)
        Text(
            text = stringResource(R.string.withdraw_manual_ready_warning),
            style = MaterialTheme.typography.body2,
            color = colorResource(R.color.notice_text),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(all = 8.dp)
                .background(colorResource(R.color.notice_background))
                .border(BorderStroke(2.dp, colorResource(R.color.notice_border)))
                .padding(all = 16.dp)
        )
        if (bankAppClick != null) {
            Button(
                onClick = bankAppClick,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_bank_button))
            }
        }
        if (onCancelClick != null) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(R.color.red)),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(Alignment.End),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_cancel))
            }
        }
    }
}

@Composable
fun DetailRow(label: String, content: String, copy: Boolean = true) {
    val context = LocalContext.current
    Row {
        Column(
            modifier = Modifier
                .weight(0.3f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.body1,
                fontWeight = if (copy) FontWeight.Bold else FontWeight.Normal,
            )
            if (copy) {
                IconButton(
                    onClick = { copyToClipBoard(context, label, content) },
                ) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy)) }
            }
        }
        Text(
            text = content,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .weight(0.7f)
                .then(if (copy) Modifier else Modifier.alpha(0.7f))
        )
    }
}

@Preview
@Composable
fun PreviewScreenIBAN() {
    Surface {
        ScreenIBAN(WithdrawStatus.ManualTransferRequiredIBAN(
            exchangeBaseUrl = "test.exchange.taler.net",
            uri = Uri.parse("https://taler.net"),
            iban = "ASDQWEASDZXCASDQWE",
            subject = "Taler Withdrawal P2T19EXRBY4B145JRNZ8CQTD7TCS03JE9VZRCEVKVWCP930P56WG",
            amountRaw = Amount("KUDOS", 10, 0),
            transactionId = "",
        ), {}) {}
    }
}
