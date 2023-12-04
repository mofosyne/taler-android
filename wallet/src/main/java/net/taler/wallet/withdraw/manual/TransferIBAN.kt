/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.wallet.R
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.withdraw.TransferData

@Composable
fun TransferIBAN(
    transfer: TransferData.IBAN,
    exchangeBaseUrl: String,
    transactionAmountRaw: Amount,
    transactionAmountEffective: Amount,
) {
    Column(
        modifier = Modifier.padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                R.string.withdraw_manual_ready_intro,
                transfer.amountRaw.toString()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        Text(
            text = stringResource(R.string.withdraw_manual_ready_warning),
            style = MaterialTheme.typography.bodyMedium,
            color = colorResource(R.color.notice_text),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(all = 8.dp)
                .background(colorResource(R.color.notice_background))
                .border(BorderStroke(2.dp, colorResource(R.color.notice_border)))
                .padding(all = 16.dp)
        )

        DetailRow(stringResource(R.string.withdraw_manual_ready_iban), transfer.iban)
        DetailRow(stringResource(R.string.withdraw_manual_ready_subject), transfer.subject)

        TransactionInfoComposable(
            label = stringResource(R.string.withdraw_exchange),
            info = exchangeBaseUrl,
        )

        WithdrawalAmountTransfer(
            amountRaw = transactionAmountRaw,
            amountEffective = transactionAmountEffective,
            conversionAmountRaw = transfer.amountRaw,
        )
    }
}