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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.Amount
import net.taler.common.startActivitySafe
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

class ManualWithdrawSuccessFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val transactionManager by lazy { model.transactionManager }
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
        val tid = status.transactionId
        val onCancelClick = if (tid == null) null else {
            {
                transactionManager.deleteTransaction(tid)
                findNavController().navigate(R.id.action_nav_exchange_manual_withdrawal_success_to_nav_main)
            }
        }
        setContent {
            MdcTheme {
                Surface {
                    when (status) {
                        is WithdrawStatus.ManualTransferRequiredBitcoin -> {
                            ScreenBitcoin(status, onBankAppClick, onCancelClick)
                        }
                        is WithdrawStatus.ManualTransferRequiredIBAN -> {
                            ScreenIBAN(status, onBankAppClick, onCancelClick)
                        }
                    }
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
private fun ScreenIBAN(
    status: WithdrawStatus.ManualTransferRequiredIBAN,
    bankAppClick: (() -> Unit)?,
    onCancelClick: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(all = 16.dp)
        .wrapContentWidth(CenterHorizontally)
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
                .align(CenterHorizontally)
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
        if (onCancelClick != null) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(R.color.red)),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(End),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_cancel))
            }
        }
    }
}

@Composable
private fun ScreenBitcoin(
    status: WithdrawStatus.ManualTransferRequiredBitcoin,
    bankAppClick: (() -> Unit)?,
    onCancelClick: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier
        .padding(all = 16.dp)
        .wrapContentWidth(CenterHorizontally)
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
            text = stringResource(R.string.withdraw_manual_bitcoin_ready_details_intro),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_ready_details_segwit),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        DetailRow(stringResource(R.string.withdraw_manual_ready_subject), status.subject)
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_ready_details_bitcoincore),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        BitcoinSegwitAddrs(
            status.amountRaw,
            status.account,
            status.segwitAddrs
        )
        Text(
            text = stringResource(R.string.withdraw_manual_bitcoin_ready_details_confirm,
                status.amountRaw.withCurrency(Amount.SEGWIT_MIN.currency) + Amount.SEGWIT_MIN + Amount.SEGWIT_MIN),
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        Text(
            text = stringResource(R.string.withdraw_manual_ready_warning),
            style = MaterialTheme.typography.body2,
            color = colorResource(R.color.notice_text),
            modifier = Modifier
                .align(CenterHorizontally)
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
        if (onCancelClick != null) {
            Button(
                onClick = onCancelClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = colorResource(R.color.red)),
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .align(End),
            ) {
                Text(text = stringResource(R.string.withdraw_manual_ready_cancel))
            }
        }
    }
}

@Composable
fun BitcoinSegwitAddrs(amount: Amount, addr: String, segwitAddrs: List<String>) {
    val context = LocalContext.current

    val sr = segwitAddrs.map { s -> """
${s} ${Amount.SEGWIT_MIN}
    """.trimIndent()}.joinToString(separator = "\n")
    val copyText = """
${addr} ${amount.withCurrency("BTC")}
${sr}
    """.trimIndent()

    Column {

        Row (modifier = Modifier.padding(vertical = 8.dp)){
            Column (modifier = Modifier.weight(0.3f)) {
                Text(
                    text = addr,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Normal,
                    fontSize = 3.em
                )
                Text(
                    text = amount.withCurrency("BTC").toString(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        for(sAddr in segwitAddrs) {
            Row (modifier = Modifier.padding(vertical = 8.dp)){
                Column (modifier = Modifier.weight(0.3f)) {
                    Text(
                        text = sAddr,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Normal,
                        fontSize = 3.em
                    )
                    Text(
                        text = Amount.SEGWIT_MIN.toString(),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Row (verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { copyToClipBoard(context, "this", copyText) },
            ) { Icon(Icons.Default.ContentCopy, stringResource(R.string.copy)) }
            Text (
                text = stringResource(R.string.copy),
                style = MaterialTheme.typography.body1,
            )
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
fun PreviewScreen2() {
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

@Preview
@Composable
fun PreviewScreenBitcoin() {
    Surface {
        ScreenBitcoin(WithdrawStatus.ManualTransferRequiredBitcoin(
            exchangeBaseUrl = "bitcoin.ice.bfh.ch",
            uri = Uri.parse("https://taler.net"),
            account = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            segwitAddrs = listOf<String>(
                "bc1qqleages8702xvg9qcyu02yclst24xurdrynvxq",
                "bc1qsleagehks96u7jmqrzcf0fw80ea5g57qm3m84c"
            ),
            subject = "0ZSX8SH0M30KHX8K3Y1DAMVGDQV82XEF9DG1HC4QMQ3QWYT4AF00",
            amountRaw = Amount("BITCOINBTC", 0, 14000000),
            transactionId = "",
        ), {}) {}
    }
}

private fun copyToClipBoard(context: Context, label: String, str: String) {
    val clipboard = context.getSystemService<ClipboardManager>()
    val clip = ClipData.newPlainText(label, str)
    clipboard?.setPrimaryClip(clip)
}
