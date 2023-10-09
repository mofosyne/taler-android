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

package net.taler.anastasis.ui.screens.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.ChallengeFeedback
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.shared.Utils
import net.taler.anastasis.ui.forms.EditAnswerForm
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeRecoveryViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@Composable
fun SolveChallengeScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Recovery
        ?: error("invalid reducer state type")

    val selectedChallengeUuid = reducerState.selectedChallengeUuid
    val challenge = remember(selectedChallengeUuid) {
        reducerState.recoveryInformation?.challenges?.find {
            it.uuid == selectedChallengeUuid
        } ?: error("empty challenge")
    }
    val challengeFeedback = remember(selectedChallengeUuid) {
        reducerState.challengeFeedback?.get(challenge.uuid)
    }

    val question by remember { mutableStateOf(challenge.instructions) }
    var answer by remember { mutableStateOf("") }
    var valid by remember { mutableStateOf(false) }

    WizardPage(
        title = stringResource(R.string.solve_challenge_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            when (challenge.type) {
                AuthMethod.Type.Question, AuthMethod.Type.Totp -> {
                    viewModel.reducerManager?.solveAnswerChallenge(answer)
                }
                AuthMethod.Type.Sms, AuthMethod.Type.Email -> {
                    viewModel.reducerManager?.solveAnswerChallenge(Utils.extractVerificationCode(answer))
                }
                // TODO: handle other challenge types
                else -> {}
            }
        },
        enableNext = valid,
    ) {
        Column {
            if (challengeFeedback != null && challengeFeedback !is ChallengeFeedback.Solved) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        when (challengeFeedback) {
                            is ChallengeFeedback.Solved -> return@Box
                            is ChallengeFeedback.IncorrectAnswer -> stringResource(R.string.challenge_feedback_incorrect_answer)
                            is ChallengeFeedback.CodeInFile -> challengeFeedback.displayHint
                            is ChallengeFeedback.CodeSent -> challengeFeedback.displayHint
                            is ChallengeFeedback.Unsupported -> stringResource(R.string.challenge_feedback_unsupported)
                            is ChallengeFeedback.RateLimitExceeded -> stringResource(R.string.challenge_feedback_rate_limit_exceeded)
                            is ChallengeFeedback.BankTransferRequired -> stringResource(R.string.challenge_feedback_bank_transfer_required)
                            is ChallengeFeedback.ServerFailure -> stringResource(R.string.challenge_feedback_server_failure)
                            is ChallengeFeedback.TruthUnknown -> stringResource(R.string.challenge_feedback_truth_unknown)
                            is ChallengeFeedback.TalerPaymentRequired -> stringResource(R.string.challenge_feedback_taler_payment_required)
                        },
                        modifier = Modifier.padding(LocalSpacing.current.small),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
            Box(Modifier.padding(LocalSpacing.current.medium)) {
                when (challenge.type) {
                    AuthMethod.Type.Question -> EditAnswerForm(
                        question = question,
                        answerLabel = stringResource(R.string.answer),
                        answer = answer,
                        onAnswerEdited = { a, v ->
                            answer = a
                            valid = v
                        }
                    )

                    AuthMethod.Type.Sms, AuthMethod.Type.Email -> EditAnswerForm(
                        answerLabel = stringResource(R.string.code),
                        answer = answer,
                        onAnswerEdited = { a, v ->
                            answer = a
                            valid = v
                        },
                        regex = "^A-\\d{5}-\\d{3}-\\d{4}-\\d{3}$",
                    )

                    AuthMethod.Type.Totp -> EditAnswerForm(
                        answerLabel = stringResource(R.string.code_totp),
                        answer = answer,
                        onAnswerEdited = { a, v ->
                            answer = a
                            valid = v
                        },
                        regex = "\\d{8}",
                        keyboardType = KeyboardType.NumberPassword,
                    )

                    // TODO: handle other challenge types
                    else -> {}
                }
            }
        }
    }
}

@Preview
@Composable
fun SolveChallengeScreenPreview() {
    SolveChallengeScreen(
        viewModel = FakeRecoveryViewModel(
            recoveryState = RecoveryStates.ChallengeSolving,
        )
    )
}