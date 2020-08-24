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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.taler.common.ContractMerchant
import net.taler.common.ContractProduct
import net.taler.lib.common.Amount
import net.taler.lib.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer
import net.taler.wallet.transactions.WithdrawalDetails.TalerBankIntegrationApi

@Serializable
data class Transactions(val transactions: List<Transaction>)

@Serializable
sealed class Transaction {
    abstract val transactionId: String
    abstract val timestamp: Timestamp
    abstract val pending: Boolean
    abstract val error: TransactionError?
    abstract val amountRaw: Amount
    abstract val amountEffective: Amount

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

@Serializable
data class TransactionError(
    private val ec: Int,
    private val hint: String? = null,
) {
    val text get() = if (hint == null) "$ec" else "$ec $hint"
}

@Serializable
@SerialName("withdrawal")
class TransactionWithdrawal(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val pending: Boolean,
    val exchangeBaseUrl: String,
    val withdrawalDetails: WithdrawalDetails,
    override val error: TransactionError? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount
) : Transaction() {
    override val icon = R.drawable.transaction_withdrawal
    override val detailPageLayout = R.layout.fragment_transaction_withdrawal

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context) = cleanExchange(exchangeBaseUrl)
    override val generalTitleRes = R.string.withdraw_title
    val confirmed: Boolean
        get() = !pending && (
                (withdrawalDetails is TalerBankIntegrationApi && withdrawalDetails.confirmed) ||
                        withdrawalDetails is ManualTransfer
                )
}

@Serializable
sealed class WithdrawalDetails {
    @Serializable
    @SerialName("manual-transfer")
    class ManualTransfer(
        /**
         * Payto URIs that the exchange supports.
         *
         * Already contains the amount and message.
         */
        val exchangePaytoUris: List<String>
    ) : WithdrawalDetails()

    @Serializable
    @SerialName("taler-bank-integration-api")
    class TalerBankIntegrationApi(
        /**
         * Set to true if the bank has confirmed the withdrawal, false if not.
         * An unconfirmed withdrawal usually requires user-input
         * and should be highlighted in the UI.
         * See also bankConfirmationUrl below.
         */
        val confirmed: Boolean,

        /**
         * If the withdrawal is unconfirmed, this can include a URL for user-initiated confirmation.
         */
        val bankConfirmationUrl: String? = null,
    ) : WithdrawalDetails()
}

@Serializable
@SerialName("payment")
class TransactionPayment(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val pending: Boolean,
    val info: TransactionInfo,
    val status: PaymentStatus,
    override val error: TransactionError? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount
) : Transaction() {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageLayout = R.layout.fragment_transaction_payment

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context) = info.merchant.name
    override val generalTitleRes = R.string.payment_title
}

@Serializable
class TransactionInfo(
    val orderId: String,
    val merchant: ContractMerchant,
    val summary: String,
    @SerialName("summary_i18n")
    val summaryI18n: Map<String, String>? = null,
    val products: List<ContractProduct>,
    val fulfillmentUrl: String? = null,
    /**
     * Message shown to the user after the payment is complete.
     * TODO actually show this
     */
    val fulfillmentMessage: String? = null,
    /**
     * Map from IETF BCP 47 language tags to localized fulfillment messages
     */
    val fulfillmentMessage_i18n: Map<String, String>? = null,
)

@Serializable
enum class PaymentStatus {
    @SerialName("aborted")
    Aborted,

    @SerialName("failed")
    Failed,

    @SerialName("paid")
    Paid,

    @SerialName("accepted")
    Accepted
}

@Serializable
@SerialName("refund")
class TransactionRefund(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val pending: Boolean,
    val refundedTransactionId: String,
    val info: TransactionInfo,
    /**
     * Part of the refund that couldn't be applied because the refund permissions were expired
     */
    val amountInvalid: Amount? = null,
    override val error: TransactionError? = null,
    @SerialName("amountEffective") // TODO remove when fixed in wallet-core
    override val amountRaw: Amount,
    @SerialName("amountRaw") // TODO remove when fixed in wallet-core
    override val amountEffective: Amount
) : Transaction() {
    override val icon = R.drawable.transaction_refund
    override val detailPageLayout = R.layout.fragment_transaction_payment

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_refund_from, info.merchant.name)
    }

    override val generalTitleRes = R.string.refund_title
}

@Serializable
@SerialName("tip")
class TransactionTip(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val pending: Boolean,
    // TODO status: TipStatus,
    val exchangeBaseUrl: String,
    val merchant: ContractMerchant,
    override val error: TransactionError? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount
) : Transaction() {
    override val icon = R.drawable.transaction_tip_accepted // TODO different when declined
    override val detailPageLayout = R.layout.fragment_transaction_payment

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_tip_from, merchant.name)
    }

    override val generalTitleRes = R.string.tip_title
}

@Serializable
@SerialName("refresh")
class TransactionRefresh(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val pending: Boolean,
    val exchangeBaseUrl: String,
    override val error: TransactionError? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount
) : Transaction() {
    override val icon = R.drawable.transaction_refresh
    override val detailPageLayout = R.layout.fragment_transaction_withdrawal

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_refresh)
    }

    override val generalTitleRes = R.string.transaction_refresh
}
