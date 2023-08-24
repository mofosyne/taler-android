package net.taler.anastasis.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import net.taler.common.Amount

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("state")
sealed class ChallengeFeedback {
    @Serializable
    @SerialName("solved")
    object Solved: ChallengeFeedback()

    @Serializable
    @SerialName("incorrect-answer")
    object IncorrectAnswer: ChallengeFeedback()

    @Serializable
    @SerialName("code-in-file")
    data class CodeInFile(
        val filename: String,
        @SerialName("display_hint")
        val displayHint: String,
    ): ChallengeFeedback()

    @Serializable
    @SerialName("code-sent")
    data class CodeSent(
        @SerialName("display_hint")
        val displayHint: String,
        @SerialName("address_hint")
        val addressHint: String,
    ): ChallengeFeedback()

    @Serializable
    @SerialName("unsupported")
    data class Unsupported(
        @SerialName("unsupported_method")
        val unsupportedMethod: String,
    ): ChallengeFeedback()

    @Serializable
    @SerialName("rate-limit-exceeded")
    object RateLimitExceeded : ChallengeFeedback()

    @Serializable
    @SerialName("iban-instructions")
    data class BankTransferRequired(
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
    class ServerFailure(
        val httpStatus: Int,
        // TODO: error_response
    ): ChallengeFeedback()

    @Serializable
    @SerialName("truth-unknown")
    object TruthUnknown: ChallengeFeedback()

    @Serializable
    @SerialName("taler-payment")
    class TalerPaymentRequired(
        @SerialName("taler_pay_uri")
        val talerPayUri: String,
        val provider: String,
        @SerialName("payment_secret")
        val paymentSecret: String,
    ): ChallengeFeedback()
}