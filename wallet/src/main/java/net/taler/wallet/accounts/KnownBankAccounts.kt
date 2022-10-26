/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.accounts

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

@Serializable
data class KnownBankAccounts(
    val accounts: List<KnownBankAccountsInfo>,
)

@Serializable
data class KnownBankAccountsInfo(
    val uri: PaytoUri,
    @SerialName("kyc_completed")
    val kycCompleted: Boolean,
    val currency: String,
    val alias: String,
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("targetType")
sealed class PaytoUri(
    val isKnown: Boolean,
    val targetType: String,
) {
    abstract val targetPath: String
    abstract val params: Map<String, String>
}

@Serializable
@SerialName("iban")
class PaytoUriIBAN(
    val iban: String,
    override val targetPath: String,
    override val params: Map<String, String>,
) : PaytoUri(
    isKnown = true,
    targetType = "iban",
)

@Serializable
@SerialName("x-taler-bank")
class PaytoUriTalerBank(
    val host: String,
    val account: String,
    override val targetPath: String,
    override val params: Map<String, String>,
) : PaytoUri(
    isKnown = true,
    targetType = "x-taler-bank",
)

@Serializable
@SerialName("bitcoin")
class PaytoUriBitcoin(
    @SerialName("segwitAddrs")
    val segwitAddresses: List<String>,
    override val targetPath: String,
    override val params: Map<String, String>,
) : PaytoUri(
    isKnown = true,
    targetType = "bitcoin",
)
