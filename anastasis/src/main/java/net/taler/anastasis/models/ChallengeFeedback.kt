package net.taler.anastasis.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.taler.common.Amount

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("state")
abstract class ChallengeFeedback

@Serializable
@SerialName("solved")
class ChallengeFeedbackSolved: ChallengeFeedback()

@Serializable
@SerialName("incorrect-answer")
class ChallengeFeedbackIncorrectAnswer: ChallengeFeedback()

@Serializable
@SerialName("code-in-file")
data class ChallengeFeedbackCodeInFile(
    val filename: String,
    @SerialName("display_hint")
    val displayHint: String,
): ChallengeFeedback()

@Serializable
@SerialName("code-sent")
data class ChallengeFeedbackCodeSent(
    @SerialName("display_hint")
    val displayHint: String,
    @SerialName("address_hint")
    val addressHint: String,
): ChallengeFeedback()

@Serializable
@SerialName("unsupported")
data class ChallengeFeedbackUnsupported(
    @SerialName("unsupported_method")
    val unsupportedMethod: String,
): ChallengeFeedback()

@Serializable
@SerialName("rate-limit-exceeded")
class ChallengeFeedbackRateLimitExceeded: ChallengeFeedback()

@Serializable
@SerialName("iban-instructions")
data class ChallengeFeedbackBankTransferRequired(
    @SerialName("challenge_amount")
    val challengeAmount: Amount,
    @SerialName("target_iban")
    val targetIban: String,
    @SerialName("target_business_name")
    val targetBusinessName: String,
    @SerialName("wire_transfer_subject")
    val wireTransferSubject: String,
    @SerialName("answer_code")
    val answerCode: Int,
): ChallengeFeedback()

@Serializable
@SerialName("server-failure")
class ChallengeFeedbackServerFailure(
    // TODO: http_status
    // TODO: error_response
): ChallengeFeedback()

@Serializable
@SerialName("truth-unknown")
class ChallengeFeedbackTruthUnknown: ChallengeFeedback()

@Serializable
@SerialName("taler-payment")
class ChallengeFeedbackTalerPaymentRequired(
    @SerialName("taler_pay_uri")
    val talerPayUri: String,
    val provider: String,
    @SerialName("payment_secret")
    val paymentSecret: String,
): ChallengeFeedback()