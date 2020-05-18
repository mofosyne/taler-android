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

package net.taler.wallet.transactions

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName
import net.taler.common.Amount
import net.taler.common.ContractMerchant
import net.taler.common.ContractProduct
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.cleanExchange

@JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
@JsonSubTypes(
    Type(value = TransactionWithdrawal::class, name = "withdrawal"),
    Type(value = TransactionPayment::class, name = "payment"),
    Type(value = TransactionRefund::class, name = "refund"),
    Type(value = TransactionTip::class, name = "tip"),
    Type(value = TransactionRefresh::class, name = "refresh")
)
abstract class Transaction(
    val transactionId: String,
    val timestamp: Timestamp,
    val pending: Boolean,
    val amountRaw: Amount,
    val amountEffective: Amount
) {
    @get:DrawableRes
    abstract val icon: Int

    @get:LayoutRes
    abstract val detailPageLayout: Int

    abstract val amountType: AmountType

    abstract fun getTitle(context: Context): String

    @get:StringRes
    abstract val generalTitleRes: Int
}

sealed class AmountType {
    object Positive : AmountType()
    object Negative : AmountType()
    object Neutral : AmountType()
}

@JsonTypeName("withdrawal")
class TransactionWithdrawal(
    transactionId: String,
    timestamp: Timestamp,
    pending: Boolean,
    val exchangeBaseUrl: String,
    val confirmed: Boolean,
    val bankConfirmationUrl: String?,
    amountRaw: Amount,
    amountEffective: Amount
) : Transaction(transactionId, timestamp, pending, amountRaw, amountEffective) {
    override val icon = R.drawable.transaction_withdrawal
    override val detailPageLayout = R.layout.fragment_transaction_withdrawal
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context) = cleanExchange(exchangeBaseUrl)
    override val generalTitleRes = R.string.withdraw_title
}

@JsonTypeName("payment")
class TransactionPayment(
    transactionId: String,
    timestamp: Timestamp,
    pending: Boolean,
    val info: TransactionInfo,
    val status: PaymentStatus,
    amountRaw: Amount,
    amountEffective: Amount
) : Transaction(transactionId, timestamp, pending, amountRaw, amountEffective) {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageLayout = R.layout.fragment_transaction_payment
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context) = info.merchant.name
    override val generalTitleRes = R.string.payment_title
}

class TransactionInfo(
    val orderId: String,
    val merchant: ContractMerchant,
    val summary: String,
    @get:JsonProperty("summary_i18n")
    val summaryI18n: Map<String, String>?,
    val products: List<ContractProduct>,
    val fulfillmentUrl: String
)

enum class PaymentStatus {
    @JsonProperty("aborted")
    Aborted,

    @JsonProperty("failed")
    Failed,

    @JsonProperty("paid")
    Paid,

    @JsonProperty("accepted")
    Accepted
}

@JsonTypeName("refund")
class TransactionRefund(
    transactionId: String,
    timestamp: Timestamp,
    pending: Boolean,
    val refundedTransactionId: String,
    val info: TransactionInfo,
    val amountInvalid: Amount,
    amountRaw: Amount,
    amountEffective: Amount
) : Transaction(transactionId, timestamp, pending, amountRaw, amountEffective) {
    override val icon = R.drawable.transaction_refund
    override val detailPageLayout = R.layout.fragment_transaction_payment
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_refund_from, info.merchant.name)
    }
    override val generalTitleRes = R.string.refund_title
}

@JsonTypeName("tip")
class TransactionTip(
    transactionId: String,
    timestamp: Timestamp,
    pending: Boolean,
    // TODO status: TipStatus,
    val exchangeBaseUrl: String,
    val merchant: ContractMerchant,
    amountRaw: Amount,
    amountEffective: Amount
) : Transaction(transactionId, timestamp, pending, amountRaw, amountEffective) {
    override val icon = R.drawable.transaction_tip_accepted // TODO different when declined
    override val detailPageLayout = R.layout.fragment_transaction_payment
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_tip_from, merchant.name)
    }
    override val generalTitleRes = R.string.tip_title
}

@JsonTypeName("refresh")
class TransactionRefresh(
    transactionId: String,
    timestamp: Timestamp,
    pending: Boolean,
    val exchangeBaseUrl: String,
    amountRaw: Amount,
    amountEffective: Amount
) : Transaction(transactionId, timestamp, pending, amountRaw, amountEffective) {
    override val icon = R.drawable.transaction_refresh
    override val detailPageLayout = R.layout.fragment_transaction_payment
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_refresh)
    }
    override val generalTitleRes = R.string.transaction_refresh
}
