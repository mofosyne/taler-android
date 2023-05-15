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

package net.taler.wallet.transactions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionState(
    val major: TransactionMajorState,
    val minor: TransactionMinorState? = null,
) {
    override fun equals(other: Any?): Boolean {
        return if (other is TransactionState)
            // if other.minor is null, then ignore minor in comparison
            major == other.major && (other.minor == null || minor == other.minor)
        else false
    }

    override fun hashCode(): Int {
        var result = major.hashCode()
        result = 31 * result + (minor?.hashCode() ?: 0)
        return result
    }
}

@Serializable
enum class TransactionMajorState {
    @SerialName("none")
    None,

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

    @SerialName("dialog")
    Dialog,

    @SerialName("suspended-aborting")
    SuspendedAborting,

    @SerialName("failed")
    Failed,

    @SerialName("deleted")
    Deleted,

    @SerialName("unknown")
    Unknown;
}

@Serializable
enum class TransactionMinorState {
    @SerialName("unknown")
    Unknown,

    @SerialName("deposit")
    Deposit,

    @SerialName("kyc")
    KycRequired,

    @SerialName("aml")
    AmlRequired,

    @SerialName("merge-kyc")
    MergeKycRequired,

    @SerialName("track")
    Track,

    @SerialName("submit-payment")
    SubmitPayment,

    @SerialName("rebind-session")
    RebindSession,

    @SerialName("refresh")
    Refresh,

    @SerialName("pickup")
    Pickup,

    @SerialName("auto-refund")
    AutoRefund,

    @SerialName("user")
    User,

    @SerialName("bank")
    Bank,

    @SerialName("exchange")
    Exchange,

    @SerialName("claim-proposal")
    ClaimProposal,

    @SerialName("check-refund")
    CheckRefund,

    @SerialName("create-purse")
    CreatePurse,

    @SerialName("delete-purse")
    DeletePurse,

    @SerialName("ready")
    Ready,

    @SerialName("merge")
    Merge,

    @SerialName("repurchase")
    Repurchase,

    @SerialName("bank-register-reserve")
    BankRegisterReserve,

    @SerialName("bank-confirm-transfer")
    BankConfirmTransfer,

    @SerialName("withdraw-coins")
    WithdrawCoins,

    @SerialName("exchange-wait-reserve")
    ExchangeWaitReserve,

    @SerialName("aborting-bank")
    AbortingBank,

    @SerialName("refused")
    Refused,

    @SerialName("withdraw")
    Withdraw,

    @SerialName("merchant-order-proposed")
    MerchantOrderProposed,

    @SerialName("proposed")
    Proposed,

    @SerialName("refund-available")
    RefundAvailable,

    @SerialName("accept-refund")
    AcceptRefund
}
