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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.MethodSpec
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.dialogs.EditMethodDialog
import net.taler.anastasis.ui.dialogs.EditProviderDialog
import net.taler.anastasis.ui.reusable.components.ActionCard
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI
import net.taler.common.CryptoUtils

@Composable
fun SelectAuthMethodsScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
    showManageProviders: Boolean = false,
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer state type")

    val tasks by viewModel.tasks.collectAsState()
    val isLoading = tasks.isBackgroundLoading

    val authProviders = reducerState.authenticationProviders ?: emptyMap()
    val selectedMethods = reducerState.authenticationMethods ?: emptyList()

    // Get only known methods of providers with "ok" status
    val availableMethods = remember(authProviders) {
        authProviders.flatMap { entry ->
            if (entry.value is AuthenticationProviderStatus.Ok) {
                (entry.value as AuthenticationProviderStatus.Ok).methods.map { it.type }
                    .filter { it != AuthMethod.Type.Unknown }
            } else emptyList()
        }.distinct()
    }

    var manageProviders by remember { mutableStateOf(showManageProviders) }

    WizardPage(
        title = if (manageProviders)
            stringResource(R.string.backup_providers)
            else stringResource(R.string.select_auth_methods_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager?.next()
        },
        actions = {
            IconButton(onClick = {
                manageProviders = !manageProviders
            }) {
                if (manageProviders) {
                    Icon(
                        Icons.Default.EditOff,
                        contentDescription = stringResource(R.string.select_auth_methods_title)
                    )
                } else {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.manage_backup_providers),
                    )
                }

            }
        },
        isLoading = isLoading,
    ) { scroll ->
        if (manageProviders) {
            ManageBackupProviders(
                nestedScrollConnection = scroll,
                authProviders = authProviders,
                onAddProvider = {
                    viewModel.reducerManager?.addProvider(it)
                },
                onDeleteProvider = {
                    viewModel.reducerManager?.deleteProvider(it)
                },
            )
        } else {
            AuthMethods(
                nestedScrollConnection = scroll,
                availableMethods = availableMethods,
                selectedMethods = selectedMethods,
                onAddMethod = {
                    viewModel.reducerManager?.addAuthentication(it.copy(
                        challenge = CryptoUtils.encodeCrock(it.challenge.toByteArray(Charsets.UTF_8))
                    ))
                },
                onDeleteMethod = {
                    viewModel.reducerManager?.deleteAuthentication(it)
                },
            )
        }
    }
}

@Composable
private fun AuthMethods(
    nestedScrollConnection: NestedScrollConnection,
    availableMethods: List<AuthMethod.Type>,
    selectedMethods: List<AuthMethod>,
    onAddMethod: (type: AuthMethod) -> Unit,
    onDeleteMethod: (index: Int) -> Unit,
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var methodType by remember { mutableStateOf<AuthMethod.Type?>(null) }
    var method by remember { mutableStateOf<AuthMethod?>(null) }
    val reset = {
        showEditDialog = false
        methodType = null
        method = null
    }

    if (showEditDialog) {
        EditMethodDialog(
            type = methodType,
            method = method,
            onCancel = {
                reset()
            },
            onMethodEdited = {
                reset()
                onAddMethod(it)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        items(items = availableMethods) { method ->
            AddMethodCard(
                modifier = Modifier
                    .padding(
                        start = LocalSpacing.current.medium,
                        end = LocalSpacing.current.medium,
                        bottom = LocalSpacing.current.small,
                    )
                    .fillMaxWidth(),
                type = method,
                onClick = {
                    methodType = it
                    showEditDialog = true
                },
            )
        }

        item {
            Divider(Modifier.padding(bottom = LocalSpacing.current.small))
        }

        items(count = selectedMethods.size) { i ->
            ChallengeCard(
                modifier = Modifier
                    .padding(
                        start = LocalSpacing.current.medium,
                        end = LocalSpacing.current.medium,
                        bottom = LocalSpacing.current.small,
                    )
                    .fillMaxWidth(),
                authMethod = selectedMethods[i],
                onDelete = { onDeleteMethod(i) },
            )
        }
    }
}

@Composable
private fun AddMethodCard(
    modifier: Modifier = Modifier,
    type: AuthMethod.Type,
    onClick: (type: AuthMethod.Type) -> Unit,
) {
    ActionCard(
        modifier = modifier,
        icon = { Icon(type.icon, contentDescription = null) },
        headline = when (type) {
            AuthMethod.Type.Question -> stringResource(R.string.add_auth_method_question)
            AuthMethod.Type.Sms -> stringResource(R.string.add_auth_method_sms)
            AuthMethod.Type.Email -> stringResource(R.string.add_auth_method_email)
            AuthMethod.Type.Iban -> stringResource(R.string.add_auth_method_iban)
            AuthMethod.Type.Mail -> stringResource(R.string.add_auth_method_mail)
            AuthMethod.Type.Totp -> stringResource(R.string.add_auth_method_totp)
            AuthMethod.Type.Unknown -> error("unknown auth method type")
        },
        onClick = { onClick(type) },
    )
}

@Composable
private fun ChallengeCard(
    modifier: Modifier = Modifier,
    authMethod: AuthMethod,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(LocalSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(authMethod.type.icon, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(authMethod.instructions, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBackupProviders(
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
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(url, style = MaterialTheme.typography.labelLarge)
                if (status is AuthenticationProviderStatus.Ok) {
                    Text(status.businessName, style = MaterialTheme.typography.labelMedium)
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

private val fakeViewModel = FakeReducerViewModel(
    state = ReducerState.Backup(
        backupState = BackupStates.AuthenticationsEditing,
        authenticationMethods = listOf(
            AuthMethod(
                type = AuthMethod.Type.Question,
                mimeType = "text/plain",
                challenge = "E1QPPS8A",
                instructions = "What is your favorite GNU package?",
            ),
            AuthMethod(
                type = AuthMethod.Type.Email,
                instructions = "E-mail to user@*le.com",
                challenge = "ENSPAWJ0CNW62VBGDHJJWRVFDM50",
            )
        ),
        authenticationProviders = mapOf(
            "http://localhost:8088/" to AuthenticationProviderStatus.Ok(
                httpStatus = 200,
                methods = listOf(
                    MethodSpec(type = AuthMethod.Type.Question, usageFee = "EUR:0.001"),
                    MethodSpec(type = AuthMethod.Type.Sms, usageFee = "EUR:0.55"),
                ),
                annualFee = "EUR:0.99",
                truthUploadFee = "EUR:3.99",
                liabilityLimit = "EUR:1",
                currency = "EUR",
                storageLimitInMegabytes = 1,
                businessName = "Anastasis 4",
                providerSalt = "CXAPCKSH9D3MYJTS9536RHJHCW",
            ),
            "http://localhost:8089/" to AuthenticationProviderStatus.Ok(
                httpStatus = 200,
                methods = listOf(
                    MethodSpec(type = AuthMethod.Type.Question, usageFee = "EUR:0.001"),
                    MethodSpec(type = AuthMethod.Type.Sms, usageFee = "EUR:0.55"),
                ),
                annualFee = "EUR:0.99",
                truthUploadFee = "EUR:3.99",
                liabilityLimit = "EUR:1",
                currency = "EUR",
                storageLimitInMegabytes = 1,
                businessName = "Anastasis 2",
                providerSalt = "CXAPCKSH9D3MYJTS9536RHJHCW",
            ),
        ),
    ),
)

@Preview
@Composable
fun SelectAuthMethodsScreenPreview() {
    SelectAuthMethodsScreen(
        viewModel = fakeViewModel,
    )
}

@Preview
@Composable
fun ManageBackupProvidersPreview() {
    SelectAuthMethodsScreen(
        viewModel = fakeViewModel,
        showManageProviders = true,
    )
}