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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.ChallengeFeedback
import net.taler.anastasis.models.ChallengeInfo
import net.taler.anastasis.models.RecoveryInformation
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeRecoveryViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@Composable
fun SelectChallengeScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Recovery
        ?: error("invalid reducer state type")

    val policies = reducerState.recoveryInformation?.policies ?: emptyList()
    val challenges = reducerState.recoveryInformation?.challenges ?: emptyList()
    val challengeFeedback = reducerState.challengeFeedback ?: emptyMap()

    WizardPage(
        title = stringResource(R.string.select_challenge_title),
        showNext = false,
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {},
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = policies.size) { index ->
                PolicyCard(
                    modifier = Modifier
                        .padding(
                            start = LocalSpacing.current.medium,
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small
                        )
                        .fillMaxSize(),
                    policy = policies[index],
                    policyIndex = index,
                    challenges = challenges,
                    challengeFeedback = challengeFeedback,
                    onChallengeClick = { uuid ->
                        viewModel.reducerManager?.selectChallenge(uuid)
                    }
                )
            }
        }
    }
}

@Composable
fun PolicyCard(
    modifier: Modifier = Modifier,
    policy: List<RecoveryInformation.Policy>,
    policyIndex: Int,
    challenges: List<ChallengeInfo>,
    challengeFeedback: Map<String, ChallengeFeedback>,
    onChallengeClick: (uuid: String) -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(LocalSpacing.current.medium),
        ) {
            Text(
                stringResource(R.string.policy_n, policyIndex + 1),
                modifier = Modifier.padding(bottom = LocalSpacing.current.small),
                style = MaterialTheme.typography.titleLarge,
            )

            policy.map { it.uuid }.forEach { uuid ->
                challenges.find { it.uuid == uuid }?.let { challenge ->
                    ChallengeCard(
                        modifier = Modifier
                            .padding(top = LocalSpacing.current.small)
                            .fillMaxWidth(),
                        challenge = challenge,
                        challengeFeedback = challengeFeedback[challenge.uuid],
                        onClick = { onChallengeClick(uuid) },
                    )
                }
            }
        }
    }
}

@Composable
fun ChallengeCard(
    modifier: Modifier = Modifier,
    challenge: ChallengeInfo,
    challengeFeedback: ChallengeFeedback? = null,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            Row(
                modifier = Modifier.padding(LocalSpacing.current.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    challenge.type.icon,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = stringResource(challenge.type.nameRes),
                )
                Spacer(Modifier.width(LocalSpacing.current.medium))
                Text(
                    challenge.instructions,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(LocalSpacing.current.medium))
                when (challengeFeedback) {
                    is ChallengeFeedback.Solved -> FeedbackSolvedButton()
                    else -> Button(onClick = onClick) {
                        Text(stringResource(R.string.challenge_solve))
                    }
                }
            }

            if (challengeFeedback != null && challengeFeedback !is ChallengeFeedback.Solved) {
                Box(Modifier.padding(
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.medium,
                    start = LocalSpacing.current.medium,
                )) {
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
                        style = MaterialTheme.typography.labelMedium
                            .copy(color = MaterialTheme.colorScheme.error),
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackSolvedButton() {
    OutlinedButton(
        onClick = {},
        enabled = false,
    ) {
        val label = stringResource(R.string.challenge_feedback_solved)
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = label,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

@Preview
@Composable
fun SelectChallengeScreenPreview() {
    SelectChallengeScreen(
        viewModel = FakeRecoveryViewModel(
            recoveryState = RecoveryStates.ChallengeSelecting,
        )
    )
}