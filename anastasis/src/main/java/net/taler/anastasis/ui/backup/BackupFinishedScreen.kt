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

package net.taler.anastasis.ui.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.models.SuccessDetail
import net.taler.anastasis.shared.Utils
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeBackupViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@Composable
fun BackupFinishedScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer state type")

    // Get only providers with "ok" status
    val providers = remember(reducerState.authenticationProviders) {
        reducerState.authenticationProviders?.filter {
            it.value is AuthenticationProviderStatus.Ok
        }?.mapValues { it.value as AuthenticationProviderStatus.Ok } ?: emptyMap()
    }

    val details = reducerState.successDetails ?: emptyMap()

    WizardPage(
        title = stringResource(R.string.backup_finished_title),
        onBackClicked = { viewModel.goHome() },
        showNext = false,
        showPrev = false,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(it),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Box(
                    modifier = Modifier
                        .padding(LocalSpacing.current.large)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .fillMaxWidth(0.4f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.CloudDone,
                        modifier = Modifier.fillMaxSize(0.5f),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        contentDescription = stringResource(R.string.success),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.backup_stored_providers),
                    modifier = Modifier.padding(LocalSpacing.current.medium),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            items(items = details.keys.toList()) { url ->
                val provider = providers[url] ?: return@items
                val detail = details[url] ?: return@items
                ProviderCard(
                    modifier = Modifier
                        .padding(
                            start = LocalSpacing.current.medium,
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small,
                        )
                        .fillMaxWidth(),
                    provider = provider,
                    detail = detail,
                )
            }
        }
    }
}

@Composable
fun ProviderCard(
    modifier: Modifier,
    provider: AuthenticationProviderStatus.Ok,
    detail: SuccessDetail,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(LocalSpacing.current.medium)
        ) {
            val date = Utils.formatDate(
                Instant
                    .fromEpochMilliseconds(detail.policyExpiration.ms)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date,
            )
            Text(
                provider.businessName,
                modifier = Modifier.padding(bottom = LocalSpacing.current.small),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    R.string.backup_policy_detail,
                    detail.policyVersion,
                    date,
                )
            )
        }
    }
}

@Preview
@Composable
fun BackupFinishedScreenPreview() {
    BackupFinishedScreen(
        viewModel = FakeBackupViewModel(
            backupState = BackupStates.BackupFinished,
        )
    )
}