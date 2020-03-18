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
import androidx.annotation.StringRes
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName
import net.taler.wallet.ParsedAmount.Companion.parseAmount
import net.taler.wallet.R
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
class Timestamp(
    @JsonProperty("t_ms")
    val ms: Long
)

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

typealias History = ArrayList<HistoryEvent>

@JsonTypeInfo(
    use = NAME,
    include = PROPERTY,
    property = "type",
    defaultImpl = HistoryUnknownEvent::class
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
    Type(value = ReserveBalanceUpdatedEvent::class, name = "reserve-balance-updated"),
    Type(value = HistoryWithdrawnEvent::class, name = "withdrawn"),
    Type(value = HistoryOrderAcceptedEvent::class, name = "order-accepted"),
    Type(value = HistoryOrderRefusedEvent::class, name = "order-refused"),
    Type(value = HistoryOrderRedirectedEvent::class, name = "order-redirected"),
    Type(value = HistoryPaymentSentEvent::class, name = "payment-sent"),
    Type(value = HistoryPaymentAbortedEvent::class, name = "payment-aborted"),
    Type(value = HistoryTipAcceptedEvent::class, name = "tip-accepted"),
    Type(value = HistoryTipDeclinedEvent::class, name = "tip-declined"),
    Type(value = HistoryRefundedEvent::class, name = "refund"),
    Type(value = HistoryRefreshedEvent::class, name = "refreshed")
)
@JsonIgnoreProperties(
    value = [
        "eventId"
    ]
)
abstract class HistoryEvent(
    val timestamp: Timestamp,
    @get:LayoutRes
    open val layout: Int = R.layout.history_row,
    @get:StringRes
    open val title: Int = 0,
    @get:DrawableRes
    open val icon: Int = R.drawable.ic_account_balance,
    open val showToUser: Boolean = false
) {
    open lateinit var json: JSONObject
}


class HistoryUnknownEvent(timestamp: Timestamp) : HistoryEvent(timestamp) {
    override val title = R.string.history_event_unknown
}

@JsonTypeName("exchange-added")
class ExchangeAddedEvent(
    timestamp: Timestamp,
    val exchangeBaseUrl: String,
    val builtIn: Boolean
) : HistoryEvent(timestamp) {
    override val title = R.string.history_event_exchange_added
}

@JsonTypeName("exchange-updated")
class ExchangeUpdatedEvent(
    timestamp: Timestamp,
    val exchangeBaseUrl: String
) : HistoryEvent(timestamp) {
    override val title = R.string.history_event_exchange_updated
}


@JsonTypeName("reserve-balance-updated")
class ReserveBalanceUpdatedEvent(
    timestamp: Timestamp,
    val newHistoryTransactions: List<ReserveTransaction>,
    /**
     * Condensed information about the reserve.
     */
    val reserveShortInfo: ReserveShortInfo,
    /**
     * Amount currently left in the reserve.
     */
    val amountReserveBalance: String,
    /**
     * Amount we expected to be in the reserve at that time,
     * considering ongoing withdrawals from that reserve.
     */
    val amountExpected: String
) : HistoryEvent(timestamp) {
    override val title = R.string.history_event_reserve_balance_updated
}

@JsonTypeName("withdrawn")
class HistoryWithdrawnEvent(
    timestamp: Timestamp,
    /**
     * Exchange that was withdrawn from.
     */
    val exchangeBaseUrl: String,
    /**
     * Unique identifier for the withdrawal session, can be used to
     * query more detailed information from the wallet.
     */
    val withdrawSessionId: String,
    val withdrawalSource: WithdrawalSource,
    /**
     * Amount that has been subtracted from the reserve's balance
     * for this withdrawal.
     */
    val amountWithdrawnRaw: String,
    /**
     * Amount that actually was added to the wallet's balance.
     */
    val amountWithdrawnEffective: String
) : HistoryEvent(timestamp) {
    override val layout = R.layout.history_receive
    override val title = R.string.history_event_withdrawn
    override val icon = R.drawable.history_withdrawn
    override val showToUser = true
}

@JsonTypeName("order-accepted")
class HistoryOrderAcceptedEvent(
    timestamp: Timestamp,
    /**
     * Condensed info about the order.
     */
    val orderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.ic_add_circle
    override val title = R.string.history_event_order_accepted
}

@JsonTypeName("order-refused")
class HistoryOrderRefusedEvent(
    timestamp: Timestamp,
    /**
     * Condensed info about the order.
     */
    val orderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.ic_cancel
    override val title = R.string.history_event_order_refused
}

@JsonTypeName("payment-sent")
class HistoryPaymentSentEvent(
    timestamp: Timestamp,
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
    val amountPaidWithFees: String,
    /**
     * Session ID that the payment was (re-)submitted under.
     */
    val sessionId: String?
) : HistoryEvent(timestamp) {
    override val layout = R.layout.history_payment
    override val title = R.string.history_event_payment_sent
    override val icon = R.drawable.ic_cash_usd_outline
    override val showToUser = true
}

@JsonTypeName("payment-aborted")
class HistoryPaymentAbortedEvent(
    timestamp: Timestamp,
    /**
     * Condensed info about the order that we already paid for.
     */
    val orderShortInfo: OrderShortInfo,
    /**
     * Amount that was lost due to refund and refreshing fees.
     */
    val amountLost: String
) : HistoryEvent(timestamp) {
    override val layout = R.layout.history_payment
    override val title = R.string.history_event_payment_aborted
    override val icon = R.drawable.history_payment_aborted
    override val showToUser = true
}

@JsonTypeName("refreshed")
class HistoryRefreshedEvent(
    timestamp: Timestamp,
    /**
     * Amount that is now available again because it has
     * been refreshed.
     */
    val amountRefreshedEffective: String,
    /**
     * Amount that we spent for refreshing.
     */
    val amountRefreshedRaw: String,
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
) : HistoryEvent(timestamp) {
    override val layout = R.layout.history_payment
    override val icon = R.drawable.history_refresh
    override val title = R.string.history_event_refreshed
    override val showToUser =
        !(parseAmount(amountRefreshedRaw) - parseAmount(amountRefreshedEffective)).isZero()
}

@JsonTypeName("order-redirected")
class HistoryOrderRedirectedEvent(
    timestamp: Timestamp,
    /**
     * Condensed info about the new order that contains a
     * product (identified by the fulfillment URL) that we've already paid for.
     */
    val newOrderShortInfo: OrderShortInfo,
    /**
     * Condensed info about the order that we already paid for.
     */
    val alreadyPaidOrderShortInfo: OrderShortInfo
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.ic_directions
    override val title = R.string.history_event_order_redirected
}

@JsonTypeName("tip-accepted")
class HistoryTipAcceptedEvent(
    timestamp: Timestamp,
    /**
     * Unique identifier for the tip to query more information.
     */
    val tipId: String,
    /**
     * Raw amount of the tip, without extra fees that apply.
     */
    val tipRaw: String
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.history_tip_accepted
    override val title = R.string.history_event_tip_accepted
    override val layout = R.layout.history_receive
    override val showToUser = true
}

@JsonTypeName("tip-declined")
class HistoryTipDeclinedEvent(
    timestamp: Timestamp,
    /**
     * Unique identifier for the tip to query more information.
     */
    val tipId: String,
    /**
     * Raw amount of the tip, without extra fees that apply.
     */
    val tipAmount: String
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.history_tip_declined
    override val title = R.string.history_event_tip_declined
    override val layout = R.layout.history_receive
    override val showToUser = true
}

@JsonTypeName("refund")
class HistoryRefundedEvent(
    timestamp: Timestamp,
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
    val amountRefundedInvalid: String,
    /**
     * Amount that has been refunded by the merchant.
     */
    val amountRefundedRaw: String,
    /**
     * Amount will be added to the wallet's balance after fees and refreshing.
     */
    val amountRefundedEffective: String
) : HistoryEvent(timestamp) {
    override val icon = R.drawable.history_refund
    override val title = R.string.history_event_refund
    override val layout = R.layout.history_receive
    override val showToUser = true
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
    val amount: String,
    /**
     * Summary of the proposal, given by the merchant.
     */
    val summary: String
)
