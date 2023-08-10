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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.AggregatedPolicyMetaInfo
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.models.SelectedVersionInfo
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@Composable
fun SelectSecretScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Recovery
        ?: error("invalid reducer state type")

    val tasks by viewModel.tasks.collectAsState()
    val isLoading = tasks.isBackgroundLoading

    val versions = reducerState.discoveryState?.aggregatedPolicies ?: emptyList()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    WizardPage(
        title = stringResource(R.string.select_secret_title),
        enableNext = selectedIndex != null,
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            selectedIndex?.let {
                viewModel.reducerManager?.selectVersion(versions[it])
            }
        },
        isLoading = isLoading,
    ) {
        LazyColumn {
            items(count = versions.size) { index ->
                SecretCard(
                    modifier = Modifier
                        .padding(
                            start = LocalSpacing.current.medium,
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small,
                        )
                        .fillMaxWidth(),
                    policy = versions[index],
                    isSelected = selectedIndex == index,
                ) { selectedIndex = index }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretCard(
    modifier: Modifier = Modifier,
    policy: AggregatedPolicyMetaInfo,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
        onClick = onSelected,
    ) {
        Row(
            modifier = Modifier
                .padding(LocalSpacing.current.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected,
            )
            Spacer(Modifier.width(LocalSpacing.current.small))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                if (policy.secretName != null) {
                    Text(
                        policy.secretName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(LocalSpacing.current.small))
                }
                Text(
                    policy.policyHash
                        .trimStart('0'),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview
@Composable
fun SelectSecretScreenPreview() {
    SelectSecretScreen(
        viewModel = FakeReducerViewModel(
            state = ReducerState.Recovery(
                recoveryState = RecoveryStates.SecretSelecting,
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
            ),
        ),
    )
}