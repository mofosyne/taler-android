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
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonElement
import net.taler.common.Amount
import net.taler.common.ContractMerchant
import net.taler.common.ContractProduct
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.TAG
import net.taler.wallet.backend.TalerErrorCode
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.cleanExchange
import net.taler.wallet.transactions.TransactionMajorState.Failed
import net.taler.wallet.transactions.TransactionMajorState.Pending
import net.taler.wallet.transactions.WithdrawalDetails.ManualTransfer
import net.taler.wallet.transactions.WithdrawalDetails.TalerBankIntegrationApi
import java.util.UUID

@Serializable
data class Transactions(
    @Serializable(with = TransactionListSerializer::class)
    val transactions: List<Transaction>,
)

class TransactionListSerializer : KSerializer<List<Transaction>> {
    private val serializer = ListSerializer(TransactionSerializer())
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): List<Transaction> {
        return decoder.decodeSerializableValue(serializer)
    }

    override fun serialize(encoder: Encoder, value: List<Transaction>) {
        throw NotImplementedError()
    }
}

class TransactionSerializer : KSerializer<Transaction> {

    private val serializer = Transaction.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor
    private val jsonSerializer = MapSerializer(String.serializer(), JsonElement.serializer())

    override fun deserialize(decoder: Decoder): Transaction {
        return try {
            decoder.decodeSerializableValue(serializer)
        } catch (e: SerializationException) {
            Log.e(TAG, "Error deserializing transaction.", e)
            DummyTransaction(
                transactionId = UUID.randomUUID().toString(),
                timestamp = Timestamp.now(),
                error = TalerErrorInfo(
                    code = TalerErrorCode.UNKNOWN,
                    message = e.message,
                    extra = decoder.decodeSerializableValue(jsonSerializer)
                ),
            )
        }
    }

    override fun serialize(encoder: Encoder, value: Transaction) {
        throw NotImplementedError()
    }
}

@Serializable
sealed class Transaction {
    abstract val transactionId: String
    abstract val timestamp: Timestamp
    abstract val txState: TransactionState
    abstract val txActions: List<TransactionAction>
    abstract val error: TalerErrorInfo?
    abstract val amountRaw: Amount
    abstract val amountEffective: Amount

    @get:DrawableRes
    abstract val icon: Int

    @get:IdRes
    abstract val detailPageNav: Int

    abstract val amountType: AmountType

    abstract fun getTitle(context: Context): String

    @get:StringRes
    abstract val generalTitleRes: Int
}

@Serializable
enum class ExtendedStatus {
    @SerialName("pending")
    Pending,

    @SerialName("done")
    Done,

    @SerialName("aborting")
    Aborting,

    @SerialName("aborted")
    Aborted,

    @SerialName("suspended")
    Suspended,

    @SerialName("failed")
    Failed,

    @SerialName("deleted")
    Deleted;
}

@Serializable
enum class TransactionAction {
    // Common States
    @SerialName("delete")
    Delete,

    @SerialName("suspend")
    Suspend,

    @SerialName("resume")
    Resume,

    @SerialName("abort")
    Abort,

    @SerialName("fail")
    Fail,

    @SerialName("retry")
    Retry,
}

sealed class AmountType {
    object Positive : AmountType()
    object Negative : AmountType()
    object Neutral : AmountType()
}

@Serializable
@SerialName("withdrawal")
class TransactionWithdrawal(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val exchangeBaseUrl: String,
    val withdrawalDetails: WithdrawalDetails,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
) : Transaction() {
    override val icon = R.drawable.transaction_withdrawal

    override val detailPageNav = R.id.action_nav_transactions_detail_withdrawal

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context) = cleanExchange(exchangeBaseUrl)
    override val generalTitleRes = R.string.withdraw_title
    val confirmed: Boolean
        get() = txState.major != Pending && (
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
        val exchangePaytoUris: List<String>,
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
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val info: TransactionInfo,
    val status: PaymentStatus,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
) : Transaction() {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageNav = R.id.action_nav_transactions_detail_payment

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
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val refundedTransactionId: String,
    val info: TransactionInfo,
    /**
     * Part of the refund that couldn't be applied because the refund permissions were expired
     */
    val amountInvalid: Amount? = null,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
) : Transaction() {
    override val icon = R.drawable.transaction_refund
    override val detailPageNav = R.id.action_nav_transactions_detail_refund

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
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val merchantBaseUrl: String,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
) : Transaction() {
    override val icon = R.drawable.transaction_tip_accepted
    override val detailPageNav = R.id.action_nav_transactions_detail_tip

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_tip_from, merchantBaseUrl)
    }

    override val generalTitleRes = R.string.tip_title
}

@Serializable
@SerialName("refresh")
class TransactionRefresh(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
) : Transaction() {
    override val icon = R.drawable.transaction_refresh
    override val detailPageNav = R.id.action_nav_transactions_detail_refresh

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_refresh)
    }

    override val generalTitleRes = R.string.transaction_refresh
}

@Serializable
@SerialName("deposit")
class TransactionDeposit(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
    val targetPaytoUri: String,
    val depositGroupId: String,
) : Transaction() {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageNav = R.id.action_nav_transactions_detail_deposit

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_deposit)
    }

    override val generalTitleRes = R.string.transaction_deposit
}

@Serializable
data class PeerInfoShort(
    val expiration: Timestamp? = null,
    val summary: String? = null,
)

/**
 * Debit because we paid someone's invoice.
 */
@Serializable
@SerialName("peer-pull-debit")
class TransactionPeerPullDebit(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val exchangeBaseUrl: String,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
    val info: PeerInfoShort,
) : Transaction() {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageNav = R.id.nav_transactions_detail_peer

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_peer_pull_debit)
    }

    override val generalTitleRes = R.string.transaction_peer_pull_debit
}

/**
 * Credit because someone paid for an invoice we created.
 */
@Serializable
@SerialName("peer-pull-credit")
class TransactionPeerPullCredit(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val exchangeBaseUrl: String,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
    val info: PeerInfoShort,
    val talerUri: String,
    // val completed: Boolean, maybe
) : Transaction() {
    override val icon = R.drawable.transaction_withdrawal
    override val detailPageNav = R.id.nav_transactions_detail_peer

    override val amountType get() = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_peer_pull_credit)
    }

    override val generalTitleRes = R.string.transaction_peer_pull_credit
}

/**
 * Debit because we sent money to someone.
 */
@Serializable
@SerialName("peer-push-debit")
class TransactionPeerPushDebit(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val exchangeBaseUrl: String,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
    val info: PeerInfoShort,
    val talerUri: String,
    // val completed: Boolean, definitely
) : Transaction() {
    override val icon = R.drawable.ic_cash_usd_outline
    override val detailPageNav = R.id.nav_transactions_detail_peer

    @Transient
    override val amountType = AmountType.Negative
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_peer_push_debit)
    }

    override val generalTitleRes = R.string.payment_title
}

/**
 * We received money via a peer payment.
 */
@Serializable
@SerialName("peer-push-credit")
class TransactionPeerPushCredit(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val txState: TransactionState,
    override val txActions: List<TransactionAction>,
    val exchangeBaseUrl: String,
    override val error: TalerErrorInfo? = null,
    override val amountRaw: Amount,
    override val amountEffective: Amount,
    val info: PeerInfoShort,
) : Transaction() {
    override val icon = R.drawable.transaction_withdrawal
    override val detailPageNav = R.id.nav_transactions_detail_peer

    @Transient
    override val amountType = AmountType.Positive
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_peer_push_credit)
    }

    override val generalTitleRes = R.string.transaction_peer_push_credit
}

/**
 * This represents a transaction that we can not parse for some reason.
 */
class DummyTransaction(
    override val transactionId: String,
    override val timestamp: Timestamp,
    override val error: TalerErrorInfo,
) : Transaction() {
    override val txState: TransactionState = TransactionState(Failed)
    override val txActions: List<TransactionAction> = listOf(TransactionAction.Delete)
    override val amountRaw: Amount = Amount.zero("TESTKUDOS")
    override val amountEffective: Amount = Amount.zero("TESTKUDOS")
    override val icon: Int = R.drawable.ic_bug_report
    override val detailPageNav: Int = R.id.nav_transactions_detail_dummy
    override val amountType: AmountType = AmountType.Neutral
    override val generalTitleRes: Int = R.string.transaction_dummy_title
    override fun getTitle(context: Context): String {
        return context.getString(R.string.transaction_dummy_title)
    }
}
