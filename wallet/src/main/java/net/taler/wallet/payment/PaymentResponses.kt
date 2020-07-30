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

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName
import net.taler.common.ContractTerms

@JsonTypeInfo(use = NAME, property = "status")
sealed class PreparePayResponse(open val proposalId: String) {
    @JsonTypeName("payment-possible")
    data class PaymentPossibleResponse(
        override val proposalId: String,
        val contractTerms: ContractTerms
    ) : PreparePayResponse(proposalId)

    @JsonTypeName("insufficient-balance")
    data class InsufficientBalanceResponse(
        override val proposalId: String,
        val contractTerms: ContractTerms
    ) : PreparePayResponse(proposalId)

    @JsonTypeName("already-confirmed")
    data class AlreadyConfirmedResponse(
        override val proposalId: String,
        /**
         * Did the payment succeed?
         */
        val paid: Boolean,

        /**
         * Redirect URL for the fulfillment page, only given if paid==true.
         */
        val nextUrl: String?
    ) : PreparePayResponse(proposalId)
}
