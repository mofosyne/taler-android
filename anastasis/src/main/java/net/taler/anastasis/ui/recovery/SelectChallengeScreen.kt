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

package net.taler.anastasis.ui.recovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.ChallengeInfo
import net.taler.anastasis.models.RecoveryInformation
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeReducerViewModel
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
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
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
            Button(onClick = onClick) {
                Text(stringResource(R.string.challenge_solve))
            }
        }
    }
}

@Preview
@Composable
fun SelectChallengeScreenPreview() {
    SelectChallengeScreen(
        viewModel = FakeReducerViewModel(
            state = ReducerState.Recovery(
                recoveryState = RecoveryStates.ChallengeSelecting,
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
            ),
        ),
    )
}