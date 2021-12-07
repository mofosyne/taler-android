/*
 * This file is part of GNU Taler
 * (C) 2020 Taler Systems S.A.
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

package net.taler.wallet.withdraw

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.startActivitySafe
import net.taler.lib.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.cleanExchange

class ManualWithdrawSuccessFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        val status = withdrawManager.withdrawStatus.value as WithdrawStatus.ManualTransferRequired
        val intent = Intent().apply {
            data = status.uri
        }
        // TODO test if this works with an actual payto:// handling app
        val componentName = intent.resolveActivity(requireContext().packageManager)
        val onBankAppClick = if (componentName == null) null else {
            { startActivitySafe(intent) }
        }
        setContent {
            MdcTheme {
                Surface {
                    Screen(status, onBankAppClick)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.withdraw_title)
    }
}

@Composable
private fun Screen(
    status: WithdrawStatus.ManualTransferRequired,
    bankAppClick: (() -> Unit)?,
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(all = 16.dp)
        .wrapContentWidth(CenterHorizontally)
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
        Row {
            Text(
                text = stringResource(R.string.withdraw_manual_ready_iban),
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.3f)
            )
            Text(
                text = status.iban,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.7f)
            )
        }
        Row {
            Text(
                text = stringResource(R.string.withdraw_manual_ready_subject),
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.3f)
            )
            Text(
                text = status.subject,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.7f)
            )
        }
        Row {
            Text(
                text = stringResource(R.string.amount_chosen),
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.3f)
            )
            Text(
                text = status.amountRaw.toString(),
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.7f)
            )
        }
        Row {
            Text(
                text = stringResource(R.string.withdraw_exchange),
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.3f)
            )
            Text(
                text = cleanExchange(status.exchangeBaseUrl),
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .weight(0.7f)
                    .alpha(0.7f)
            )
        }
        Text(
            text = stringResource(R.string.withdraw_manual_ready_warning),
            style = MaterialTheme.typography.body2,
            color = colorResource(R.color.notice_text),
            modifier = Modifier
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
                    .align(CenterHorizontally),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_bank_button))
            }
        }
    }
}

@Preview
@Composable
fun PreviewScreen() {
    Surface {
        Screen(WithdrawStatus.ManualTransferRequired(
            exchangeBaseUrl = "test.exchange.taler.net",
            uri = Uri.parse("https://taler.net"),
            iban = "ASDQWEASDZXCASDQWE",
            subject = "Taler Withdrawal P2T19EXRBY4B145JRNZ8CQTD7TCS03JE9VZRCEVKVWCP930P56WG",
            amountRaw = Amount("KUDOS", 10, 0)
        )) {}
    }
}
