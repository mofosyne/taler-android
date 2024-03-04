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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.wallet.backend.TalerErrorInfo

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("status")
sealed class PreparePayResponse {

    @Serializable
    @SerialName("payment-possible")
    data class PaymentPossibleResponse(
        val transactionId: String,
        val amountRaw: Amount,
        val amountEffective: Amount,
        val contractTerms: ContractTerms,
    ) : PreparePayResponse() {
        fun toPayStatusPrepared() = PayStatus.Prepared(
            contractTerms = contractTerms,
            transactionId = transactionId,
            amountRaw = amountRaw,
            amountEffective = amountEffective,
        )
    }

    @Serializable
    @SerialName("insufficient-balance")
    data class InsufficientBalanceResponse(
        val amountRaw: Amount,
        val contractTerms: ContractTerms,
    ) : PreparePayResponse()

    @Serializable
    @SerialName("already-confirmed")
    data class AlreadyConfirmedResponse(
        val transactionId: String,
        /**
         * Did the payment succeed?
         */
        val paid: Boolean,
        val amountRaw: Amount,
        val amountEffective: Amount? = null,
        val contractTerms: ContractTerms,
    ) : PreparePayResponse()
}

@Serializable
sealed class ConfirmPayResult {
    @Serializable
    @SerialName("done")
    data class Done(
        val transactionId: String,
        val contractTerms: ContractTerms,
    ) : ConfirmPayResult()

    @Serializable
    @SerialName("pending")
    data class Pending(
        val transactionId: String,
        val lastError: TalerErrorInfo? = null,
    ) : ConfirmPayResult()
}
