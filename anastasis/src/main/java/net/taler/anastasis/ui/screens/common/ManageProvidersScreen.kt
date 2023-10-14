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

package net.taler.anastasis.ui.screens.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.dialogs.EditProviderDialog
import net.taler.anastasis.ui.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeBackupViewModel

@Composable
fun ManageProvidersScreen(
    nestedScrollConnection: NestedScrollConnection,
    authProviders: Map<String, AuthenticationProviderStatus>,
    onAddProvider: (url: String) -> Unit,
    onDeleteProvider: (url: String) -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        EditProviderDialog(
            onProviderEdited = onAddProvider,
            onCancel = { showEditDialog = false }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showEditDialog = true
            }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.add),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .nestedScroll(nestedScrollConnection)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
        ) {
            items(items = authProviders.keys.toList()) { url ->
                val status = authProviders[url]!!
                ProviderCard(
                    modifier = Modifier
                        .padding(
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small,
                            start = LocalSpacing.current.medium,
                        )
                        .fillMaxWidth(),
                    url = url,
                    status = status,
                    onDelete = { onDeleteProvider(url) },
                )
            }
        }
    }
}

@Composable
fun ProviderCard(
    modifier: Modifier = Modifier,
    url: String,
    status: AuthenticationProviderStatus,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(LocalSpacing.current.medium)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (status) {
                is AuthenticationProviderStatus.Ok -> Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.provider_status_ok),
                    tint = MaterialTheme.colorScheme.primary,
                )
                is AuthenticationProviderStatus.Disabled -> Icon(
                    Icons.Default.SyncDisabled,
                    contentDescription = stringResource(R.string.provider_status_disabled),
                    tint = MaterialTheme.colorScheme.primary,
                )
                is AuthenticationProviderStatus.Error -> Icon(
                    Icons.Default.Error,
                    contentDescription = stringResource(R.string.provider_status_error),
                    tint = MaterialTheme.colorScheme.error,
                )
                is AuthenticationProviderStatus.NotContacted -> Icon(
                    Icons.Default.QuestionMark,
                    contentDescription = stringResource(R.string.provider_status_not_contacted),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = LocalSpacing.current.medium)
                    .weight(1f),
            ) {
                Text(url, style = MaterialTheme.typography.labelLarge)
                if (status is AuthenticationProviderStatus.Ok) {
                    Spacer(Modifier.height(LocalSpacing.current.small))
                    Text(status.businessName, style = MaterialTheme.typography.labelLarge)
                    ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                        if (!status.annualFee.isZero()) {
                            Spacer(Modifier.height(LocalSpacing.current.small))
                            Text(
                                stringResource(
                                    R.string.provider_annual_fee,
                                    status.annualFee.toString()
                                )
                            )
                        }
                        if (!status.truthUploadFee.isZero()) {
                            Spacer(Modifier.height(LocalSpacing.current.small))
                            Text(
                                stringResource(
                                    R.string.provider_truth_upload_fee,
                                    status.truthUploadFee.toString()
                                )
                            )
                        }
                        if (!status.liabilityLimit.isZero()) {
                            Spacer(Modifier.height(LocalSpacing.current.small))
                            Text(
                                stringResource(
                                    R.string.provider_liability_limit,
                                    status.liabilityLimit.toString()
                                )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

@Preview
@Composable
fun ManageProvidersScreenPreview() {
    val viewModel by remember {
        mutableStateOf(
            FakeBackupViewModel(
                backupState = BackupStates.AuthenticationsEditing,
            ),
        )
    }
    val reducerState by viewModel.reducerState.collectAsState()
    val authProviders = (reducerState as ReducerState.Backup).authenticationProviders
    WizardPage(title = stringResource(R.string.manage_backup_providers)) {
        ManageProvidersScreen(
            nestedScrollConnection = it,
            authProviders = authProviders!!,
            onAddProvider = {},
            onDeleteProvider = {},
        )
    }
}