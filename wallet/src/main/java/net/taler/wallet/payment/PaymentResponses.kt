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

package net.taler.wallet.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.ContractTerms
import net.taler.lib.android.CustomClassDiscriminator
import net.taler.lib.common.Amount
import net.taler.wallet.transactions.TransactionError

@Serializable
sealed class PreparePayResponse {
    companion object : CustomClassDiscriminator {
        override val discriminator: String = "status"
    }

    @Serializable
    @SerialName("payment-possible")
    data class PaymentPossibleResponse(
        val proposalId: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val contractTerms: ContractTerms,
    ) : PreparePayResponse() {
        fun toPayStatusPrepared() = PayStatus.Prepared(
            contractTerms = contractTerms,
            proposalId = proposalId,
            amountRaw = amountRaw,
            amountEffective = amountEffective,
        )
    }

    @Serializable
    @SerialName("insufficient-balance")
    data class InsufficientBalanceResponse(
        val proposalId: String,
        val amountRaw: Amount,
        val contractTerms: ContractTerms,
    ) : PreparePayResponse()

    @Serializable
    @SerialName("already-confirmed")
    data class AlreadyConfirmedResponse(
        val proposalId: String,
        /**
         * Did the payment succeed?
         */
        val paid: Boolean,
        val amountRaw: Amount,
        val amountEffective: Amount,

        /**
         * Redirect URL for the fulfillment page, only given if paid==true.
         */
        val nextUrl: String?,
    ) : PreparePayResponse()
}

@Serializable
sealed class ConfirmPayResult {
    @Serializable
    @SerialName("done")
    data class Done(val nextUrl: String) : ConfirmPayResult()

    @Serializable
    @SerialName("pending")
    data class Pending(val lastError: TransactionError) : ConfirmPayResult()
}
