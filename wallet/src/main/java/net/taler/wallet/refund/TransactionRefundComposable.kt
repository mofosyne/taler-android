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
import net.taler.common.CurrencySpecification
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
import net.taler.wallet.transactions.TransactionInfoComposable
import net.taler.wallet.transactions.TransactionMajorState.Pending
import net.taler.wallet.transactions.TransactionRefund
import net.taler.wallet.transactions.TransactionState
import net.taler.wallet.transactions.TransitionsComposable

@Composable
fun TransactionRefundComposable(
    t: TransactionRefund,
    devMode: Boolean,
    spec: CurrencySpecification?,
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
            label = stringResource(id = R.string.transaction_refund),
            amount = t.amountEffective.withSpec(spec),
            amountType = AmountType.Positive,
        )
        TransactionAmountComposable(
            label = stringResource(id = R.string.transaction_order_total),
            amount = t.amountRaw.withSpec(spec),
            amountType = AmountType.Neutral,
        )
        val fee = t.amountRaw - t.amountEffective
        if (!fee.isZero()) {
            TransactionAmountComposable(
                label = stringResource(id = R.string.withdraw_fees),
                amount = fee.withSpec(spec),
                amountType = AmountType.Negative,
            )
        }
        TransactionInfoComposable(
            label = stringResource(id = R.string.transaction_order),
            info = t.paymentInfo?.summary ?: "",
        )
        TransitionsComposable(t, devMode, onTransition)
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
        txState = TransactionState(Pending),
        txActions = listOf(Retry, Suspend, Abort),
        paymentInfo = RefundPaymentInfo(
            merchant = MerchantInfo(name = "Taler"),
            summary = "Some Product that was bought and can have quite a long label",
        ),
        refundedTransactionId = "transactionId",
        amountRaw = Amount.fromString("TESTKUDOS", "42.23"),
        amountEffective = Amount.fromString("TESTKUDOS", "42.1337"),
        error = TalerErrorInfo(code = TalerErrorCode.WALLET_WITHDRAWAL_KYC_REQUIRED),
    )
    TalerSurface {
        TransactionRefundComposable(t = t, devMode = true, spec = null) {}
    }
}
