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

package net.taler.anastasis.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.taler.anastasis.backend.TalerErrorInfo
import net.taler.anastasis.backend.Tasks
import net.taler.anastasis.models.AggregatedPolicyMetaInfo
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.ChallengeFeedback
import net.taler.anastasis.models.ChallengeInfo
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.models.CoreSecret
import net.taler.anastasis.models.CountryInfo
import net.taler.anastasis.models.MethodSpec
import net.taler.anastasis.models.Policy
import net.taler.anastasis.models.RecoveryInformation
import net.taler.anastasis.models.RecoveryInternalData
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.models.SelectedVersionInfo
import net.taler.anastasis.models.SuccessDetail
import net.taler.anastasis.models.UserAttributeSpec
import net.taler.common.Amount
import net.taler.common.Timestamp

open class FakeReducerViewModel(
    state: ReducerState,
    error: TalerErrorInfo? = null,
): ReducerViewModelI {
    override val reducerManager = null
    private val _reducerState = MutableStateFlow<ReducerState?>(state)
    override val reducerState: StateFlow<ReducerState?> = _reducerState.asStateFlow()
    private val _reducerError = MutableStateFlow(error)
    override val reducerError: StateFlow<TalerErrorInfo?> = _reducerError.asStateFlow()
    private val _loading = MutableStateFlow(Tasks())
    override val tasks = _loading.asStateFlow()

    override fun goBack(): Boolean = false

    override fun goHome() {
        _reducerState.value = null
    }

    override fun cleanError() {
        _reducerError.value = null
    }
}

internal val continents = listOf(
    ContinentInfo(name = "Europe"),
    ContinentInfo(name = "North America"),
)

internal val countries = listOf(
    CountryInfo(
        code = "ch",
        name = "Switzerland",
        continent = "Europe",
    ),
    CountryInfo(
        code = "de",
        name = "Germany",
        continent = "Europe",
    )
)

internal val identityAttributes = mapOf(
    "full_name" to "Max Musterman",
    "birthdate" to "2000-01-01",
)

internal val authenticationProviders = mapOf(
    "http://localhost:8088/" to AuthenticationProviderStatus.Ok(
        httpStatus = 200,
        methods = listOf(
            MethodSpec(type = AuthMethod.Type.Question, usageFee = Amount.fromJSONString("EUR:0.001")),
            MethodSpec(type = AuthMethod.Type.Sms, usageFee = Amount.fromJSONString("EUR:0.55")),
        ),
        annualFee = Amount.fromJSONString("EUR:0.99"),
        truthUploadFee = Amount.fromJSONString("EUR:3.99"),
        liabilityLimit = Amount.fromJSONString("EUR:1"),
        currency = "EUR",
        storageLimitInMegabytes = 1,
        businessName = "Anastasis 4",
        providerSalt = "CXAPCKSH9D3MYJTS9536RHJHCW",
    ),
    "http://localhost:8089/" to AuthenticationProviderStatus.Ok(
        httpStatus = 200,
        methods = listOf(
            MethodSpec(type = AuthMethod.Type.Question, usageFee = Amount.fromJSONString("EUR:0.001")),
            MethodSpec(type = AuthMethod.Type.Sms, usageFee = Amount.fromJSONString("EUR:0.55")),
        ),
        annualFee = Amount.fromJSONString("EUR:0.99"),
        truthUploadFee = Amount.fromJSONString("EUR:3.99"),
        liabilityLimit = Amount.fromJSONString("EUR:1"),
        currency = "EUR",
        storageLimitInMegabytes = 1,
        businessName = "Anastasis 2",
        providerSalt = "CXAPCKSH9D3MYJTS9536RHJHCW",
    )
)

internal val requiredAttributes = listOf(
    UserAttributeSpec(
        type = "string",
        name = "full_name",
        label = "Full name",
        widget = "anastasis_gtk_ia_full_name",
        uuid = "9e8f463f-575f-42cb-85f3-759559997331",
    ),
    UserAttributeSpec(
        type = "date",
        name = "birthdate",
        label = "Birthdate",
        widget = "anastasis_gtk_ia_birthdate",
        uuid = "83d655c7-bdb6-484d-904e-80c1058c8854",
    ),
)

internal const val selectedContinent = "Europe"
internal const val selectedCountry = "ch"

internal val coreSecret = CoreSecret(
    value = "EDJP6WK5EG50",
    mime = "text/plain",
)

class FakeBackupViewModel(
    backupState: BackupStates,
): FakeReducerViewModel(
    state = ReducerState.Backup(
        backupState = backupState,
        continents = continents,
        countries = countries,
        identityAttributes = identityAttributes,
        authenticationProviders = authenticationProviders,
        authenticationMethods = listOf(
            AuthMethod(
                type = AuthMethod.Type.Question,
                mimeType = "text/plain",
                challenge = "E1QPPS8A",
                instructions = "What is your favorite GNU package?",
            ),
            AuthMethod(
                type = AuthMethod.Type.Email,
                instructions = "E-mail to user@*le.com",
                challenge = "ENSPAWJ0CNW62VBGDHJJWRVFDM50",
            )
        ),
        requiredAttributes = requiredAttributes,
        selectedContinent = selectedContinent,
        selectedCountry = selectedCountry,
        secretName = "_TALERWALLET_MyPinePhone",
        policies = listOf(
            Policy(
                methods = listOf(
                    Policy.PolicyMethod(
                        authenticationMethod = 0,
                        provider = "http://localhost:8089/",
                    ),
                    Policy.PolicyMethod(
                        authenticationMethod = 1,
                        provider = "http://localhost:8088/",
                    ),
                ),
            ),
            Policy(
                methods = listOf(
                    Policy.PolicyMethod(
                        authenticationMethod = 0,
                        provider = "http://localhost:8089/",
                    ),
                ),
            ),
        ),
        successDetails = mapOf(
            "http://localhost:8088/" to SuccessDetail(
                policyVersion = 1,
                policyExpiration = Timestamp.now(),
            ),
        ),
        coreSecret = coreSecret,
        uploadFees = listOf(
            ReducerState.Backup.UploadFee(
                fee = Amount("KUDOS", 42L, 0),
            ),
        ),
    ),
)

class FakeRecoveryViewModel(
    recoveryState: RecoveryStates,
): FakeReducerViewModel(
    state = ReducerState.Recovery(
        recoveryState = recoveryState,
        continents = continents,
        countries = countries,
        identityAttributes = identityAttributes,
        selectedContinent = selectedContinent,
        selectedCountry = selectedCountry,
        requiredAttributes = requiredAttributes,
        recoveryInformation = RecoveryInformation(
            challenges = listOf(
                ChallengeInfo(
                    instructions = "What is your favorite GNU package?",
                    type = AuthMethod.Type.Question,
                    uuid = "RNB84NQZPCM3MZWF9D5FFMSYYN07J2NAT5N8Q0DBHHT7R3GJ4AA0",
                ),
                ChallengeInfo(
                    instructions = "E-mail to user@*le.com",
                    type = AuthMethod.Type.Email,
                    uuid = "ZA6T35B8XAR0DNKS5H100GK8PDPTA7Q8ST2FPQSYAZ4QRAA9XKK0",
                ),
            ),
            policies = listOf(
                listOf(
                    RecoveryInformation.Policy(uuid = "RNB84NQZPCM3MZWF9D5FFMSYYN07J2NAT5N8Q0DBHHT7R3GJ4AA0"),
                    RecoveryInformation.Policy(uuid = "ZA6T35B8XAR0DNKS5H100GK8PDPTA7Q8ST2FPQSYAZ4QRAA9XKK0")
                ),
            ),
        ),
        recoveryDocument = RecoveryInternalData(
            secretName = "Secret",
            providerUrl = "http://localhost:8089",
            version = 1,
        ),
        selectedChallengeUuid = "RNB84NQZPCM3MZWF9D5FFMSYYN07J2NAT5N8Q0DBHHT7R3GJ4AA0",
        challengeFeedback = mapOf(
            "RNB84NQZPCM3MZWF9D5FFMSYYN07J2NAT5N8Q0DBHHT7R3GJ4AA0" to ChallengeFeedback.IncorrectAnswer,
            "ZA6T35B8XAR0DNKS5H100GK8PDPTA7Q8ST2FPQSYAZ4QRAA9XKK0" to ChallengeFeedback.Solved,
        ),
        coreSecret = coreSecret,
        authenticationProviders = authenticationProviders,
        discoveryState = ReducerState.Recovery.DiscoveryState(
            state = "finished",
            aggregatedPolicies = listOf(
                AggregatedPolicyMetaInfo(
                    attributeMask = 0,
                    policyHash = "000000000000000000000000000000000000000000000000000B28GR6691Y51HR2SAFJZFF0DCMRDZD1YQMS03A55P9NCWHQGEKW8",
                    providers = listOf(
                        SelectedVersionInfo.Provider(
                            url = "https://v1.anastasis.taler.net/",
                            version = 1,
                        ),
                        SelectedVersionInfo.Provider(
                            url = "https://v1.anastasis.codeblau.de/",
                            version = 1,
                        ),
                    ),
                    secretName = "Secret",
                ),
            ),
        ),
    )
)
