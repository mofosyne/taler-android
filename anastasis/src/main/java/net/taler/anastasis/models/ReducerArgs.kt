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
        val identityAttributes: Map<String, String>
    )

    @Serializable
    data class AddProvider(
        @SerialName("provider_url")
        val providerUrl: String
    )

    @Serializable
    data class DeleteProvider(
        @SerialName("provider_url")
        val providerUrl: String
    )

    @Serializable
    data class AddAuthentication(
        @SerialName("authentication_method")
        val authenticationMethod: AuthMethod,
    )

    @Serializable
    data class DeleteAuthentication(
        @SerialName("authentication_method")
        val authenticationMethod: Int,
    )

    @Serializable
    data class DeletePolicy(
        @SerialName("policy_index")
        val policyIndex: Int,
    )

    @Serializable
    data class EnterSecretName(val name: String)

    @Serializable
    data class EnterSecret(
        val secret: CoreSecret,
        val expiration: Timestamp? = null,
    )

    @Serializable
    data class SelectContinent(val continent: String)

    @Serializable
    data class SelectCountry(
        @SerialName("country_code")
        val countryCode: String,
        val currency: String,
    )

    @Serializable
    data class SelectChallenge(val uuid: String)

    @Serializable
    sealed class SolveChallengeRequest {
        @Serializable
        data class Answer(val answer: String): SolveChallengeRequest()

        @Serializable
        data class Pin(val pin: Int): SolveChallengeRequest()

        @Serializable
        data class Hash(val hash: String): SolveChallengeRequest()
    }

    @Serializable
    data class AddPolicy(val policy: List<Policy.PolicyMethod>)

    @Serializable
    data class UpdateExpiration(val expiration: Timestamp)

    // TODO: ActionArgsChangeVersion

    @Serializable
    data class UpdatePolicy(
        @SerialName("policy_index")
        val policyIndex: Int,
        val policy: List<Policy.PolicyMethod>,
    )

}