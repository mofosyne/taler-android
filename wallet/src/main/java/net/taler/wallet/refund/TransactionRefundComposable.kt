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

package net.taler.wallet.refund

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.common.Amount
import net.taler.common.ContractMerchant
import net.taler.common.Timestamp
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorCode
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.payment.PurchaseDetails
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.DeleteTransactionComposable
import net.taler.wallet.transactions.ErrorTransactionButton
import net.taler.wallet.transactions.ExtendedStatus
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfo
import net.taler.wallet.transactions.TransactionRefund

@Composable
fun TransactionRefundComposable(
    t: TransactionRefund,
    devMode: Boolean,
    onFulfill: (url: String) -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = CenterHorizontally,
    ) {
        val context = LocalContext.current
        Text(
            modifier = Modifier.padding(16.dp),
            text = t.timestamp.ms.toAbsoluteTime(context).toString(),
            style = MaterialTheme.typography.bodyLarge,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.transaction_refund),
            amount = t.amountEffective,
            amountType = AmountType.Positive,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.transaction_order_total),
            amount = t.amountRaw,
            amountType = AmountType.Neutral,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = t.amountRaw - t.amountEffective,
            amountType = AmountType.Negative,
        )
        PurchaseDetails(info = t.info) {
            onFulfill(t.info.fulfillmentUrl ?: "")
        }
        DeleteTransactionComposable(onDelete)
        if (devMode && t.error != null) {
            ErrorTransactionButton(error = t.error)
        }
    }
}

@Preview
@Composable
fun TransactionRefundComposablePreview() {
    val t = TransactionRefund(
        transactionId = "transactionId",
        timestamp = Timestamp.fromMillis(System.currentTimeMillis() - 360 * 60 * 1000),
        extendedStatus = ExtendedStatus.Pending,
        info = TransactionInfo(
            orderId = "123",
            merchant = ContractMerchant(name = "Taler"),
            summary = "Some Product that was bought and can have quite a long label",
            fulfillmentMessage = "This is some fulfillment message",
            fulfillmentUrl = "https://bank.demo.taler.net/",
            products = listOf(),
        ),
        refundedTransactionId = "transactionId",
        amountRaw = Amount.fromString("TESTKUDOS", "42.23"),
        amountEffective = Amount.fromString("TESTKUDOS", "42.1337"),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    TalerSurface {
        TransactionRefundComposable(t = t, devMode = true, onFulfill = {}) {}
    }
}
