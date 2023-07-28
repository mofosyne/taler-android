package net.taler.anastasis.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.taler.common.Amount
import net.taler.common.Timestamp

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("reducer_type")
sealed class ReducerState {
    @Serializable
    @SerialName("backup")
    data class Backup(
        @SerialName("backup_state")
        val backupState: BackupStates,
        val continents: List<ContinentInfo>? = null,
        val countries: List<CountryInfo>? = null,
        @SerialName("identity_attributes")
        val identityAttributes: Map<String, String>? = null,
        @SerialName("authentication_providers")
        val authenticationProviders: Map<String, AuthenticationProviderStatus>? = null,
        @SerialName("authentication_methods")
        val authenticationMethods: List<AuthMethod>? = null,
        @SerialName("required_attributes")
        val requiredAttributes: List<UserAttributeSpec>? = null,
        @SerialName("selected_continent")
        val selectedContinent: String? = null,
        @SerialName("selected_country")
        val selectedCountry: String? = null,
        @SerialName("secret_name")
        val secretName: String? = null,
        val policies: List<Policy>? = null,
        @SerialName("recovery_data")
        val recoveryData: RecoveryData? = null,
        @SerialName("policy_providers")
        val policyProviders: List<PolicyProvider>? = null,
        @SerialName("success_details")
        val successDetails: Map<String, SuccessDetail>? = null,
        val payments: List<String>? = null,
        @SerialName("policy_payment_requests")
        val policyPaymentRequests: List<PolicyPaymentRequest>? = null,
        @SerialName("core_secret")
        val coreSecret: CoreSecret? = null,
        val expiration: Timestamp? = null,
        @SerialName("upload_fees")
        val uploadFees: List<UploadFee>? = null,
        @SerialName("truth_upload_payment_secrets")
        val truthUploadPaymentSecrets: Map<String, String>? = null,
    ): ReducerState() {
        @Serializable
        data class RecoveryData(
            @SerialName("truth_metadata")
            val truthMetadata: Map<String, TruthMetaData>,
            @SerialName("recovery_document")
            val recoveryDocument: RecoveryDocument,
        )

        @Serializable
        data class PolicyPaymentRequest(
            val payto: String,
            val provider: String,
        )

        @Serializable
        data class UploadFee(
            val fee: Amount,
        )
    }

    @Serializable
    @SerialName("recovery")
    data class Recovery(
        @SerialName("recovery_state")
        val recoveryState: RecoveryStates,
        @SerialName("identity_attributes")
        val identityAttributes: Map<String, String>? = null,
        val continents: List<ContinentInfo>? = null,
        val countries: List<CountryInfo>? = null,
        @SerialName("selected_continent")
        val selectedContinent: String? = null,
        @SerialName("selected_country")
        val selectedCountry: String? = null,
        @SerialName("required_attributes")
        val requiredAttributes: List<UserAttributeSpec>? = null,
        @SerialName("recovery_information")
        val recoveryInformation: RecoveryInformation? = null,
        @SerialName("recovery_document")
        val recoveryDocument: RecoveryInternalData? = null,
        @SerialName("verbatim_recovery_document")
        val verbatimRecoveryDocument: RecoveryDocument? = null,
        @SerialName("selected_challenge_uuid")
        val selectedChallengeUuid: String? = null,
        @SerialName("selected_version")
        val selectedVersion: SelectedVersionInfo? = null,
        @SerialName("challenge_feedback")
        val challengeFeedback: Map<String, ChallengeFeedback>? = null,
        // TODO: recovered_key_shares
        val coreSecret: CoreSecret? = null,
        @SerialName("authentication_providers")
        val authenticationProviders: Map<String, AuthenticationProviderStatus>? = null,
    ): ReducerState()

    @Serializable
    @SerialName("error")
    data class Error(
        val code: Int,
        val hint: String? = null,
        val detail: String? = null,
    ): ReducerState()
}

@Serializable
data class ContinentInfo(
    val name: String,
)

@Serializable
data class CountryInfo(
    val code: String,
    val name: String,
    val continent: String,
)

@Serializable
data class Policy(
    val methods: List<PolicyMethod>,
) {
    @Serializable
    data class PolicyMethod(
        @SerialName("authentication_method")
        val authenticationMethod: Int,
        val provider: String
    )
}

@Serializable
data class PolicyProvider(
    @SerialName("provider_url")
    val providerUrl: String,
)

@Serializable
data class SuccessDetail(
    @SerialName("policy_version")
    val policyVersion: Int,
    @SerialName("policy_expiration")
    val policyExpiration: Timestamp,
)

@Serializable
data class CoreSecret(
    val mime: String,
    val value: String,
    val filename: String? = null,
)

@Serializable
data class AuthMethod(
    val type: String,
    val instructions: String,
    val challenge: String,
    @SerialName("mime_type")
    val mimeType: String? = null,
)

@Serializable
data class ChallengeInfo(
    val instructions: String,
    val type: String,
    val uuid: String,
)

@Serializable
data class UserAttributeSpec(
    val label: String,
    val name: String,
    val type: String,
    val uuid: String,
    val widget: String,
    val optional: Boolean? = null,
    @SerialName("validation-regex")
    val validationRegex: String? = null,
    @SerialName("validation-logic")
    val validationLogic: String? = null,
    val autocomplete: String? = null,
)

@Serializable
data class RecoveryInternalData(
    @SerialName("secret_name")
    val secretName: String,
    @SerialName("provider_url")
    val providerUrl: String,
    val version: Int,
)

@Serializable
data class RecoveryInformation(
    val challenges: List<ChallengeInfo>,
    val policies: List<List<Policy>>,
) {
    @Serializable
    data class Policy(
        val uuid: String,
    )
}

@Serializable
data class TruthMetaData(
    val uuid: String,
    @SerialName("key_share")
    val keyShare: String,
    @SerialName("policy_index")
    val policyIndex: Int,
    @SerialName("pol_method_index")
    val polMethodIndex: Int,
    val nonce: String,
    @SerialName("truth_key")
    val truthKey: String,
    @SerialName("master_salt")
    val masterSalt: String,
)

@Serializable
enum class BackupStates {
    @SerialName("CONTINENT_SELECTING")
    ContinentSelecting,

    @SerialName("COUNTRY_SELECTING")
    CountrySelecting,

    @SerialName("USER_ATTRIBUTES_COLLECTING")
    UserAttributesCollecting,

    @SerialName("AUTHENTICATIONS_EDITING")
    AuthenticationsEditing,

    @SerialName("POLICIES_REVIEWING")
    PoliciesReviewing,

    @SerialName("SECRET_EDITING")
    SecretEditing,

    @SerialName("TRUTHS_PAYING")
    TruthsPaying,

    @SerialName("POLICIES_PAYING")
    PoliciesPaying,

    @SerialName("BACKUP_FINISHED")
    BackupFinished;
}

@Serializable
enum class RecoveryStates {
    @SerialName("CONTINENT_SELECTING")
    ContinentSelecting,

    @SerialName("COUNTRY_SELECTING")
    CountrySelecting,

    @SerialName("USER_ATTRIBUTES_COLLECTING")
    UserAttributesCollecting,

    @SerialName("SECRET_SELECTING")
    SecretSelecting,

    @SerialName("CHALLENGE_SELECTING")
    ChallengeSelecting,

    @SerialName("CHALLENGE_PAYING")
    ChallengePaying,

    @SerialName("CHALLENGE_SOLVING")
    ChallengeSolving,

    @SerialName("RECOVERY_FINISHED")
    RecoveryFinished;
}

@Serializable
data class MethodSpec(
    val type: String,
    @SerialName("usage_fee")
    val usageFee: String,
)



@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("status")
sealed class AuthenticationProviderStatus {
    @Serializable
    @SerialName("not-contacted")
    object NotContacted: AuthenticationProviderStatus()

    @Serializable
    @SerialName("ok")
    data class Ok(
        @SerialName("annual_fee")
        val annualFee: String,
        @SerialName("business_name")
        val businessName: String,
        val currency: String,
        @SerialName("http_status")
        val httpStatus: Int,
        @SerialName("liability_limit")
        val liabilityLimit: String,
        @SerialName("provider_salt")
        val providerSalt: String,
        @SerialName("storage_limit_in_megabytes")
        val storageLimitInMegabytes: Int,
        @SerialName("truth_upload_fee")
        val truthUploadFee: String,
        val methods: List<MethodSpec>,
    ): AuthenticationProviderStatus()

    @Serializable
    @SerialName("disabled")
    object Disabled : AuthenticationProviderStatus()

    @Serializable
    @SerialName("error")
    data class Error(
        @SerialName("http_status")
        val httpStatus: Int? = null,
        val code: Int,
        val hint: String? = null,
    ): AuthenticationProviderStatus()
}

// TODO: ReducerStateBackupUserAttributesCollecting

// TODO: ActionArgsEnterUserAttributes

// TODO: ActionArgsAddProvider

// TODO: ActionArgsDeleteProvider

// TODO: ActionArgsAddAuthentication

// TODO: ActionArgsDeleteAuthentication

// TODO: ActionArgsDeletePolicy

// TODO: ActionArgsEnterSecretName

// TODO: ActionArgsEnterSecret

// TODO: ActionArgsSelectContinent

// TODO: ActionArgsSelectCountry

// TODO: ActionArgsSelectChallenge

// TODO: ActionArgsSolveChallengeRequest

// TODO: SolveChallengeAnswerRequest

// TODO: SolveChallengePinRequest

// TODO: SolveChallengeHashRequest

// TODO: PolicyMember

// TODO: ActionArgsAddPolicy

// TODO: ActionArgsUpdateExpiration

// TODO: ActionArgsUpdateExpiration

@Serializable
data class SelectedVersionInfoProviders(
    val url: String,
    val version: Int,
)

@Serializable
data class SelectedVersionInfo(
    val attributeMask: Int,
    val providers: SelectedVersionInfoProviders,
)

// TODO: ActionArgsChangeVersion

// TODO: ActionArgsUpdatePolicy

// TODO: DiscoveryCursor

// TODO: PolicyMetaInfo

// TODO: AggregatedPolicyMetaInfo

// TODO: DiscoveryResult