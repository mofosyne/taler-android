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

package net.taler.wallet.history

import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.cleanExchange
import org.json.JSONObject

enum class ReserveType {
    /**
     * Manually created.
     */
    @JsonProperty("manual")
    MANUAL,

    /**
     * Withdrawn from a bank that has "tight" Taler integration
     */
    @JsonProperty("taler-bank-withdraw")
    @Suppress("unused")
    TALER_BANK_WITHDRAW,
}

@JsonInclude(NON_EMPTY)
class ReserveCreationDetail(val type: ReserveType, val bankUrl: String?)

enum class RefreshReason {
    @JsonProperty("manual")
    @Suppress("unused")
    MANUAL,

    @JsonProperty("pay")
    PAY,

    @JsonProperty("refund")
    @Suppress("unused")
    REFUND,

    @JsonProperty("abort-pay")
    @Suppress("unused")
    ABORT_PAY,

    @JsonProperty("recoup")
    @Suppress("unused")
    RECOUP,

    @JsonProperty("backup-restored")
    @Suppress("unused")
    BACKUP_RESTORED
}

@JsonInclude(NON_EMPTY)
class ReserveShortInfo(
    /**
     * The exchange that the reserve will be at.
     */
    val exchangeBaseUrl: String,
    /**
     * Key to query more details
     */
    val reservePub: String,
    /**
     * Detail about how the reserve has been created.
     */
    val reserveCreationDetail: ReserveCreationDetail
)

sealed class AmountType {
    object Positive : AmountType()
    object Negative : AmountType()
    object Neutral : AmountType()
}

class DisplayAmount(
    val amount: Amount,
    val type: AmountType
)

typealias History = ArrayList<HistoryEvent>

@JsonTypeInfo(
    use = NAME,
    include = PROPERTY,
    property = "type",
    defaultImpl = UnknownHistoryEvent::class
)
/** missing:
AuditorComplaintSent = "auditor-complained-sent",
AuditorComplaintProcessed = "auditor-complaint-processed",
AuditorTrustAdded = "auditor-trust-added",
AuditorTrustRemoved = "auditor-trust-removed",
ExchangeTermsAccepted = "exchange-terms-accepted",
ExchangePolicyChanged = "exchange-policy-changed",
ExchangeTrustAdded = "exchange-trust-added",
ExchangeTrustRemoved = "exchange-trust-removed",
FundsDepositedToSelf = "funds-deposited-to-self",
FundsRecouped = "funds-recouped",
ReserveCreated = "reserve-created",
 */
@JsonSubTypes(
    Type(value = ExchangeAddedEvent::class, name = "exchange-added"),
    Type(value = ExchangeUpdatedEvent::class, name = "exchange-updated"),
    Type(value = ReserveBalanceUpdatedHistoryEvent::class, name = "reserve-balance-updated"),
    Type(value = WithdrawHistoryEvent::class, name = "withdrawn"),
    Type(value = OrderAcceptedHistoryEvent::class, name = "order-accepted"),
    Type(value = OrderRefusedHistoryEvent::class, name = "order-refused"),
    Type(value = OrderRedirectedHistoryEvent::class, name = "order-redirected"),
    Type(value = PaymentHistoryEvent::class, name = "payment-sent"),
    Type(value = PaymentAbortedHistoryEvent::class, name = "payment-aborted"),
    Type(value = TipAcceptedHistoryEvent::class, name = "tip-accepted"),
    Type(value = TipDeclinedHistoryEvent::class, name = "tip-declined"),
    Type(value = RefundHistoryEvent::class, name = "refund"),
    Type(value = RefreshHistoryEvent::class, name = "refreshed")
)
abstract class HistoryEvent(
    val timestamp: Timestamp,
    val eventId: String,
    @get:LayoutRes
    open val detailPageLayout: Int = 0,
    @get:DrawableRes
    open val icon: Int = R.drawable.ic_account_balance,
    open val showToUser: Boolean = false
) {
    abstract val title: String?
    open lateinit var json: JSONObject
    open val displayAmount: DisplayAmount? = null
    open fun isCurrency(currency: String): Boolean = true
}


class UnknownHistoryEvent(timestamp: Timestamp, eventId: String) : HistoryEvent(timestamp, eventId) {
    override val title: String? = null
}

@JsonTypeName("exchange-added")
class ExchangeAddedEvent(
    timestamp: Timestamp,
    eventId: String,
    val exchangeBaseUrl: String,
    val builtIn: Boolean
) : HistoryEvent(timestamp, eventId) {
    override val title = cleanExchange(exchangeBaseUrl)
}

@JsonTypeName("exchange-updated")
class ExchangeUpdatedEvent(
    timestamp: Timestamp,
    eventId: String,
    val exchangeBaseUrl: String
) : HistoryEvent(timestamp, eventId) {
    override val title = cleanExchange(exchangeBaseUrl)
}


@JsonTypeName("reserve-balance-updated")
class ReserveBalanceUpdatedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed information about the reserve.
     */
    val reserveShortInfo: ReserveShortInfo,
    /**
     * Amount currently left in the reserve.
     */
    val reserveBalance: Amount,
    /**
     * Amount we expected to be in the reserve at that time,
     * considering ongoing withdrawals from that reserve.
     */
    val reserveAwaitedAmount: Amount,
    /**
     * Amount that hasn't been withdrawn yet.
     */
    val reserveUnclaimedAmount: Amount
) : HistoryEvent(timestamp, eventId) {
    override val title: String? = null
    override val displayAmount = DisplayAmount(
        reserveBalance,
        AmountType.Neutral
    )
    override fun isCurrency(currency: String) = reserveBalance.currency == currency
}

@JsonTypeName("withdrawn")
class WithdrawHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Exchange that was withdrawn from.
     */
    val exchangeBaseUrl: String,
    /**
     * Unique identifier for the withdrawal session, can be used to
     * query more detailed information from the wallet.
     */
    val withdrawalGroupId: String,
    val withdrawalSource: WithdrawalSource,
    /**
     * Amount that has been subtracted from the reserve's balance
     * for this withdrawal.
     */
    val amountWithdrawnRaw: Amount,
    /**
     * Amount that actually was added to the wallet's balance.
     */
    val amountWithdrawnEffective: Amount
) : HistoryEvent(timestamp, eventId) {
    override val detailPageLayout = R.layout.fragment_event_withdraw
    override val title = cleanExchange(exchangeBaseUrl)
    override val icon = R.drawable.transaction_withdrawal
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        amountWithdrawnEffective,
        AmountType.Positive
    )
    override fun isCurrency(currency: String) = amountWithdrawnRaw.currency == currency
}

@JsonTypeName("order-accepted")
class OrderAcceptedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed info about the order.
     */
    val orderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_add_circle
    override val title: String? = null
    override fun isCurrency(currency: String) = orderShortInfo.amount.currency == currency
}

@JsonTypeName("order-refused")
class OrderRefusedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed info about the order.
     */
    val orderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_cancel
    override val title: String? = null
    override fun isCurrency(currency: String) = orderShortInfo.amount.currency == currency
}

@JsonTypeName("payment-sent")
class PaymentHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed info about the order that we already paid for.
     */
    val orderShortInfo: OrderShortInfo,
    /**
     * Set to true if the payment has been previously sent
     * to the merchant successfully, possibly with a different session ID.
     */
    val replay: Boolean,
    /**
     * Number of coins that were involved in the payment.
     */
    val numCoins: Int,
    /**
     * Amount that was paid, including deposit and wire fees.
     */
    val amountPaidWithFees: Amount,
    /**
     * Session ID that the payment was (re-)submitted under.
     */
    val sessionId: String?
) : HistoryEvent(timestamp, eventId) {
    override val detailPageLayout = R.layout.fragment_event_paid
    override val title = orderShortInfo.summary
    override val icon = R.drawable.ic_cash_usd_outline
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        amountPaidWithFees,
        AmountType.Negative
    )
    override fun isCurrency(currency: String) = orderShortInfo.amount.currency == currency
}

@JsonTypeName("payment-aborted")
class PaymentAbortedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed info about the order that we already paid for.
     */
    val orderShortInfo: OrderShortInfo,
    /**
     * Amount that was lost due to refund and refreshing fees.
     */
    val amountLost: Amount
) : HistoryEvent(timestamp, eventId) {
    override val title = orderShortInfo.summary
    override val icon = R.drawable.transaction_payment_aborted
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        amountLost,
        AmountType.Negative
    )
    override fun isCurrency(currency: String) = orderShortInfo.amount.currency == currency
}

@JsonTypeName("refreshed")
class RefreshHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Amount that is now available again because it has
     * been refreshed.
     */
    val amountRefreshedEffective: Amount,
    /**
     * Amount that we spent for refreshing.
     */
    val amountRefreshedRaw: Amount,
    /**
     * Why was the refreshing done?
     */
    val refreshReason: RefreshReason,
    val numInputCoins: Int,
    val numRefreshedInputCoins: Int,
    val numOutputCoins: Int,
    /**
     * Identifier for a refresh group, contains one or
     * more refresh session IDs.
     */
    val refreshGroupId: String
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_refresh
    override val title: String? = null
    override val showToUser = !(amountRefreshedRaw - amountRefreshedEffective).isZero()
    override val displayAmount: DisplayAmount?
        get() {
            return if (showToUser) DisplayAmount(
                amountRefreshedRaw - amountRefreshedEffective,
                AmountType.Negative
            )
            else null
        }

    override fun isCurrency(currency: String) = amountRefreshedRaw.currency == currency
}

@JsonTypeName("order-redirected")
class OrderRedirectedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Condensed info about the new order that contains a
     * product (identified by the fulfillment URL) that we've already paid for.
     */
    val newOrderShortInfo: OrderShortInfo,
    /**
     * Condensed info about the order that we already paid for.
     */
    val alreadyPaidOrderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_directions
    override val title = newOrderShortInfo.summary
    override fun isCurrency(currency: String) = newOrderShortInfo.amount.currency == currency
}

@JsonTypeName("tip-accepted")
class TipAcceptedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Unique identifier for the tip to query more information.
     */
    val tipId: String,
    /**
     * Raw amount of the tip, without extra fees that apply.
     */
    val tipRaw: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_tip_accepted
    override val title: String? = null
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        tipRaw,
        AmountType.Positive
    )
    override fun isCurrency(currency: String) = tipRaw.currency == currency
}

@JsonTypeName("tip-declined")
class TipDeclinedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    /**
     * Unique identifier for the tip to query more information.
     */
    val tipId: String,
    /**
     * Raw amount of the tip, without extra fees that apply.
     */
    val tipAmount: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_tip_declined
    override val title: String? = null
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        tipAmount,
        AmountType.Neutral
    )
    override fun isCurrency(currency: String) = tipAmount.currency == currency
}

@JsonTypeName("refund")
class RefundHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val orderShortInfo: OrderShortInfo,
    /**
     * Unique identifier for this refund.
     * (Identifies multiple refund permissions that were obtained at once.)
     */
    val refundGroupId: String,
    /**
     * Part of the refund that couldn't be applied because
     * the refund permissions were expired.
     */
    val amountRefundedInvalid: Amount,
    /**
     * Amount that has been refunded by the merchant.
     */
    val amountRefundedRaw: Amount,
    /**
     * Amount will be added to the wallet's balance after fees and refreshing.
     */
    val amountRefundedEffective: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_refund
    override val title = orderShortInfo.summary
    override val detailPageLayout = R.layout.fragment_event_paid
    override val showToUser = true
    override val displayAmount = DisplayAmount(
        amountRefundedEffective,
        AmountType.Positive
    )
    override fun isCurrency(currency: String) = amountRefundedRaw.currency == currency
}

@JsonTypeInfo(
    use = NAME,
    include = PROPERTY,
    property = "type"
)
@JsonSubTypes(
    Type(value = WithdrawalSourceReserve::class, name = "reserve")
)
abstract class WithdrawalSource

@Suppress("unused")
@JsonTypeName("tip")
class WithdrawalSourceTip(
    val tipId: String
) : WithdrawalSource()

@JsonTypeName("reserve")
class WithdrawalSourceReserve(
    val reservePub: String
) : WithdrawalSource()

data class OrderShortInfo(
    /**
     * Wallet-internal identifier of the proposal.
     */
    val proposalId: String,
    /**
     * Order ID, uniquely identifies the order within a merchant instance.
     */
    val orderId: String,
    /**
     * Base URL of the merchant.
     */
    val merchantBaseUrl: String,
    /**
     * Amount that must be paid for the contract.
     */
    val amount: Amount,
    /**
     * Summary of the proposal, given by the merchant.
     */
    val summary: String
)
