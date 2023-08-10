package net.taler.anastasis.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChallengeType {
    @SerialName("question")
    Question,

    @SerialName("sms")
    Sms,

    @SerialName("email")
    Email,

    @SerialName("post")
    Post,

    @SerialName("totp")
    Totp,

    @SerialName("iban")
    Iban;
}

@Serializable
data class RecoveryDocument(
    @SerialName("secret_name")
    val secretName: String? = null,
    @SerialName("encrypted_core_secret")
    val encryptedCoreSecret: String,
    @SerialName("escrow_methods")
    val escrowMethods: List<EscrowMethod>,
    val policies: List<DecryptionPolicy>,
)

@Serializable
data class DecryptionPolicy(
    val salt: String,
    @SerialName("master_key")
    val masterKey: String,
    val uuids: List<String>,
)

@Serializable
data class EscrowMethod(
    val url: String,
    @SerialName("escrow_type")
    val escrowType: ChallengeType,
    val uuid: String,
    @SerialName("truth_key")
    val truthKey: String,
    @SerialName("question_salt")
    val questionSalt: String,
    @SerialName("provider_salt")
    val providerSalt: String,
    val instructions: String,
)