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
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName
import net.taler.common.Amount
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.transactions.AmountType
import org.json.JSONObject

class DisplayAmount(
    val amount: Amount,
    val type: AmountType
)

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
    @get:DrawableRes
    open val icon: Int = R.drawable.ic_account_balance
) {
    val title: String get() = this::class.java.simpleName
    open val displayAmount: DisplayAmount? = null
    lateinit var json: JSONObject
}

class UnknownHistoryEvent(timestamp: Timestamp, eventId: String) : HistoryEvent(timestamp, eventId)

@JsonTypeName("exchange-added")
class ExchangeAddedEvent(
    timestamp: Timestamp,
    eventId: String
) : HistoryEvent(timestamp, eventId)

@JsonTypeName("exchange-updated")
class ExchangeUpdatedEvent(
    timestamp: Timestamp,
    eventId: String
) : HistoryEvent(timestamp, eventId)

@JsonTypeName("reserve-balance-updated")
class ReserveBalanceUpdatedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val reserveBalance: Amount
) : HistoryEvent(timestamp, eventId) {
    override val displayAmount = DisplayAmount(reserveBalance, AmountType.Neutral)
}

@JsonTypeName("withdrawn")
class WithdrawHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val amountWithdrawnEffective: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_withdrawal
    override val displayAmount = DisplayAmount(amountWithdrawnEffective, AmountType.Positive)
}

@JsonTypeName("order-accepted")
class OrderAcceptedHistoryEvent(
    timestamp: Timestamp,
    eventId: String
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_add_circle
}

@JsonTypeName("order-refused")
class OrderRefusedHistoryEvent(
    timestamp: Timestamp,
    eventId: String
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_cancel
}

@JsonTypeName("payment-sent")
class PaymentHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val amountPaidWithFees: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_cash_usd_outline
    override val displayAmount = DisplayAmount(amountPaidWithFees, AmountType.Negative)
}

@JsonTypeName("payment-aborted")
class PaymentAbortedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    amountLost: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_payment_aborted
    override val displayAmount = DisplayAmount(amountLost, AmountType.Negative)
}

@JsonTypeName("refreshed")
class RefreshHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val amountRefreshedEffective: Amount,
    val amountRefreshedRaw: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_refresh
    override val displayAmount =
        DisplayAmount(amountRefreshedRaw - amountRefreshedEffective, AmountType.Negative)
}

@JsonTypeName("order-redirected")
class OrderRedirectedHistoryEvent(
    timestamp: Timestamp,
    eventId: String
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.ic_directions
}

@JsonTypeName("tip-accepted")
class TipAcceptedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    tipRaw: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_tip_accepted
    override val displayAmount = DisplayAmount(tipRaw, AmountType.Positive)
}

@JsonTypeName("tip-declined")
class TipDeclinedHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    tipAmount: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_tip_declined
    override val displayAmount = DisplayAmount(tipAmount, AmountType.Neutral)
}

@JsonTypeName("refund")
class RefundHistoryEvent(
    timestamp: Timestamp,
    eventId: String,
    val amountRefundedEffective: Amount
) : HistoryEvent(timestamp, eventId) {
    override val icon = R.drawable.transaction_refund
    override val displayAmount = DisplayAmount(amountRefundedEffective, AmountType.Positive)
}
