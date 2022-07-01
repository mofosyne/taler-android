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

package net.taler.wallet.tip

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.common.Timestamp
import net.taler.wallet.backend.TalerErrorInfo

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("accepted")
sealed class PrepareTipResponse {

    @Serializable
    @SerialName("false")
    data class TipPossibleResponse(
        val walletTipId: String,
        val merchantBaseUrl: String,
        val exchangeBaseUrl: String,
        val expirationTimestamp: Timestamp,
        val tipAmountRaw: Amount,
        val tipAmountEffective: Amount,
    ) : PrepareTipResponse() {
        fun toTipStatusPrepared() = TipStatus.Prepared(
            walletTipId = walletTipId,
            merchantBaseUrl = merchantBaseUrl,
            exchangeBaseUrl = exchangeBaseUrl,
            expirationTimestamp = expirationTimestamp,
            tipAmountEffective = tipAmountEffective,
            tipAmountRaw =  tipAmountRaw
        )
    }

    @Serializable
    @SerialName("true")
    data class AlreadyAcceptedResponse(
        val walletTipId: String,
    ) : PrepareTipResponse()
}

@Serializable
class ConfirmTipResult {

}
