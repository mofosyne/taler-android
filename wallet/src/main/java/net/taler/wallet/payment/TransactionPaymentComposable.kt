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
import androidx.compose.ui.Alignment
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
import net.taler.wallet.transactions.DeleteTransactionComposable
import net.taler.wallet.transactions.ErrorTransactionButton
import net.taler.wallet.transactions.ExtendedStatus
import net.taler.wallet.transactions.PaymentStatus
import net.taler.wallet.transactions.Transaction
import net.taler.wallet.transactions.TransactionAmountComposable
import net.taler.wallet.transactions.TransactionInfo
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionLinkComposable
import net.taler.wallet.transactions.TransactionPayment
import net.taler.wallet.transactions.TransactionRefund

@Composable
fun TransactionPaymentComposable(
    t: Transaction,
    devMode: Boolean?,
    onFulfill: (url: String) -> Unit,
    onDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val context = LocalContext.current
        Text(
            modifier = Modifier.padding(16.dp),
            text = t.timestamp.ms.toAbsoluteTime(context).toString(),
            style = MaterialTheme.typography.bodyLarge,
        )
        TransactionAmountComposable(
            label = stringResource(id = when (t) {
                is TransactionPayment -> R.string.transaction_paid
                else -> R.string.transaction_refund
            }),
            amount = t.amountEffective,
            amountType = when (t) {
                is TransactionPayment -> AmountType.Negative
                else -> AmountType.Positive
            },
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.transaction_order_total),
            amount = t.amountRaw,
            amountType = AmountType.Neutral,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.withdraw_fees),
            amount = when (t) {
                is TransactionPayment -> t.amountEffective - t.amountRaw
                else -> t.amountRaw - t.amountEffective
            },
            amountType = AmountType.Negative,
        )
        when (t) {
            is TransactionPayment -> t.info
            is TransactionRefund -> t.info
            else -> null
        }?.let { info ->
            PurchaseDetails(info = info) {
                onFulfill(info.fulfillmentUrl!!)
            }
        }
        DeleteTransactionComposable(onDelete)
        if (devMode == true && t.error != null) {
            ErrorTransactionButton(error = t.error!!)
        }
    }
}

@Composable
fun PurchaseDetails(
    info: TransactionInfo,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
        extendedStatus = ExtendedStatus.Pending,
        info = TransactionInfo(
            orderId = "123",
            merchant = ContractMerchant(name = "Taler"),
            summary = "Some Product that was bought and can have quite a long label",
            fulfillmentMessage = "This is some fulfillment message",
            fulfillmentUrl = "https://bank.demo.taler.net/",
            products = listOf(),
        ),
        status = PaymentStatus.Paid,
        amountRaw = Amount.fromDouble("TESTKUDOS", 42.1337),
        amountEffective = Amount.fromDouble("TESTKUDOS", 42.23),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    TalerSurface {
        TransactionPaymentComposable(t = t, devMode = true, onFulfill = {}) {}
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
        amountRaw = Amount.fromDouble("TESTKUDOS", 42.23),
        amountEffective = Amount.fromDouble("TESTKUDOS", 42.1337),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    TalerSurface {
        TransactionPaymentComposable(t = t, devMode = true, onFulfill = {}) {}
    }
}