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

package net.taler.anastasis.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.Timestamp

@Serializable
sealed class ReducerArgs {

    @Serializable
    data class EnterUserAttributes(
        @SerialName("identity_attributes")
        val identityAttributes: Map<String, String>,
    )

    // TODO: ActionArgsAddProvider

    // TODO: ActionArgsDeleteProvider

    @Serializable
    data class AddAuthentication(
        @SerialName("authentication_method")
        val authenticationMethod: AuthMethod,
    )

    // TODO: ActionArgsDeleteAuthentication

    // TODO: ActionArgsDeletePolicy

    // TODO: ActionArgsEnterSecretName

    @Serializable
    data class EnterSecret(
        val secret: Secret,
        val expiration: Timestamp? = null,
    ) {
        @Serializable
        data class Secret(
            val value: String,
            val mime: String? = null,
        )
    }

    @Serializable
    data class SelectContinent(
        val continent: String,
    )

    @Serializable
    data class SelectCountry(
        @SerialName("country_code")
        val countryCode: String,
    )

    // TODO: ActionArgsSelectChallenge

    // TODO: ActionArgsSolveChallengeRequest

    // TODO: ActionArgsAddPolicy

    // TODO: ActionArgsUpdateExpiration

    // TODO: ActionArgsUpdateExpiration

    // TODO: ActionArgsChangeVersion

    // TODO: ActionArgsUpdatePolicy

}