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

package net.taler.wallet.payment

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
import net.taler.wallet.transactions.AmountType
import net.taler.wallet.transactions.ErrorTransactionButton
import net.taler.wallet.transactions.TransactionAction
import net.taler.wallet.transactions.TransactionAction.Abort
import net.taler.wallet.transactions.TransactionAction.Retry
import net.taler.wallet.transactions.TransactionAction.Suspend
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfo
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionLinkComposable
import net.taler.wallet.transactions.TransactionMajorState.Pending
import net.taler.wallet.transactions.TransactionPayment
import net.taler.wallet.transactions.TransactionState
import net.taler.wallet.transactions.TransitionsComposable

@Composable
fun TransactionPaymentComposable(
    t: TransactionPayment,
    devMode: Boolean,
    onFulfill: (url: String) -> Unit,
    onTransition: (t: TransactionAction) -> Unit,
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
            label = stringResource(id = R.string.transaction_paid),
            amount = t.amountEffective,
            amountType = AmountType.Negative,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.transaction_order_total),
            amount = t.amountRaw,
            amountType = AmountType.Neutral,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = t.amountEffective - t.amountRaw,
            amountType = AmountType.Negative,
        )
        if (t.posConfirmation != null) {
            TransactionInfoComposable(
                label = stringResource(id = R.string.payment_confirmation_code),
                info = t.posConfirmation,
            )
        }
        PurchaseDetails(info = t.info) {
            onFulfill(t.info.fulfillmentUrl ?: "")
        }
        TransitionsComposable(t, devMode, onTransition)
        if (devMode && t.error != null) {
            ErrorTransactionButton(error = t.error)
        }
    }
}

@Composable
fun PurchaseDetails(
    info: TransactionInfo,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = CenterHorizontally,
    ) {
        // Summary and fulfillment message
        val text = if (info.fulfillmentMessage == null) {
            info.summary
        } else {
            "${info.summary}\n\n${info.fulfillmentMessage}"
        }
        if (info.fulfillmentUrl != null) {
            TransactionLinkComposable(
                label = stringResource(id = R.string.transaction_order),
                info = text,
            ) { onClick() }
        } else {
            TransactionInfoComposable(
                label = stringResource(id = R.string.transaction_order),
                info = text,
            )
        }
        // Order ID
        Text(
            stringResource(id = R.string.transaction_order_id, info.orderId),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Preview
@Composable
fun TransactionPaymentComposablePreview() {
    val t = TransactionPayment(
        transactionId = "transactionId",
        timestamp = Timestamp.fromMillis(System.currentTimeMillis() - 360 * 60 * 1000),
        txState = TransactionState(Pending),
        txActions = listOf(Retry, Suspend, Abort),
        info = TransactionInfo(
            orderId = "123",
            merchant = ContractMerchant(name = "Taler"),
            summary = "Some Product that was bought and can have quite a long label",
            fulfillmentMessage = "This is some fulfillment message",
            fulfillmentUrl = "https://bank.demo.taler.net/",
            products = listOf(),
        ),
        amountRaw = Amount.fromString("TESTKUDOS", "42.1337"),
        amountEffective = Amount.fromString("TESTKUDOS", "42.23"),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    TalerSurface {
        TransactionPaymentComposable(t = t, devMode = true, onFulfill = {}) {}
    }
}
