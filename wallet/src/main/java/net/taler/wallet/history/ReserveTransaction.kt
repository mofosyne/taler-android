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

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME
import com.fasterxml.jackson.annotation.JsonTypeName


@JsonTypeInfo(
    use = NAME,
    include = PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ReserveDepositTransaction::class, name = "DEPOSIT")
)
abstract class ReserveTransaction


@JsonTypeName("DEPOSIT")
class ReserveDepositTransaction(
    /**
     * Amount withdrawn.
     */
    val amount: String,
    /**
     * Sender account payto://-URL
     */
    @JsonProperty("sender_account_url")
    val senderAccountUrl: String,
    /**
     * Transfer details uniquely identifying the transfer.
     */
    @JsonProperty("wire_reference")
    val wireReference: String,
    /**
     * Timestamp of the incoming wire transfer.
     */
    val timestamp: Timestamp
) : ReserveTransaction()
