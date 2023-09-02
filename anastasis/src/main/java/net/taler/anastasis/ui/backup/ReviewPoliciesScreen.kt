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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material3.Badge
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.Policy
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.dialogs.EditPolicyDialog
import net.taler.anastasis.ui.forms.EditPolicyForm
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeBackupViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewPoliciesScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer type")

    val policies = reducerState.policies ?: emptyList()
    val methods = reducerState.authenticationMethods ?: emptyList()

    // Get only providers with "ok" status
    val providers = remember(reducerState.authenticationProviders) {
        reducerState.authenticationProviders?.filter {
            it.value is AuthenticationProviderStatus.Ok
        }?.mapValues { it.value as AuthenticationProviderStatus.Ok } ?: emptyMap()
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingPolicy by remember { mutableStateOf<Policy?>(null) }
    var editingPolicyIndex by remember { mutableStateOf<Int?>(null) }
    val reset = {
        showEditDialog = false
        editingPolicy = null
        editingPolicyIndex = null
    }

    if (showEditDialog) {
        EditPolicyDialog(
            policy = editingPolicy,
            methods = methods,
            providers = providers,
            onCancel = { reset() },
            onPolicyEdited = {
                editingPolicyIndex?.let { index ->
                    viewModel.reducerManager?.updatePolicy(index, it)
                } ?: run {
                    viewModel.reducerManager?.addPolicy(it)
                }
                reset()
            }
        )
    }

    WizardPage(
        title = stringResource(R.string.review_policies_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager?.next()
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showEditDialog = true },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.add),
                    )
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
            ) {
                items(count = policies.size) { index ->
                    PolicyCard(
                        modifier = Modifier.padding(
                            start = LocalSpacing.current.small,
                            end = LocalSpacing.current.small,
                            bottom = LocalSpacing.current.small,
                        ),
                        policy = policies[index],
                        methods = methods,
                        providers = providers,
                        index = index,
                        onEdit = {
                            viewModel.reducerManager?.updatePolicy(index, it)
                        },
                    ) {
                        viewModel.reducerManager?.deletePolicy(index)
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyCard(
    modifier: Modifier = Modifier,
    methods: List<AuthMethod>,
    providers: Map<String, AuthenticationProviderStatus.Ok>,
    policy: Policy,
    index: Int,
    onEdit: (policy: Policy) -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        var editing by remember{ mutableStateOf(false) }

        Column(modifier = Modifier.padding(LocalSpacing.current.medium)) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.policy_n, index + 1),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { editing = !editing }) {
                    Icon(
                        if (editing) Icons.Default.EditOff else Icons.Default.Edit,
                        contentDescription = if (editing)
                            stringResource(R.string.cancel) else stringResource(R.string.edit),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }

            if (editing) {
                EditPolicyForm(
                    policy = policy,
                    methods = methods,
                    providers = providers,
                    onPolicyEdited = { onEdit(it) },
                )
            } else {
                Column {
                    policy.methods.forEach { m ->
                        val method = methods[m.authenticationMethod]
                        val provider = providers[m.provider]
                        if (provider != null) {
                            PolicyMethodCard(
                                modifier = Modifier
                                    .padding(top = LocalSpacing.current.small)
                                    .fillMaxWidth(),
                                method = method,
                                provider = provider,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyMethodCard(
    modifier: Modifier = Modifier,
    method: AuthMethod,
    provider: AuthenticationProviderStatus.Ok,
) {
    val usageFee = remember(provider) {
        provider.methods.find { it.type == method.type }?.usageFee
    }

    OutlinedCard(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(LocalSpacing.current.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                method.type.icon,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = stringResource(method.type.nameRes),
            )
            Spacer(Modifier.width(LocalSpacing.current.medium))
            Column {
                Text(method.instructions, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(LocalSpacing.current.small))
                Text(
                    stringResource(R.string.provided_by, provider.businessName),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (usageFee != null && !usageFee.isZero()) {
                    Spacer(Modifier.height(LocalSpacing.current.small))
                    Badge {
                        Text(usageFee.toString())
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ReviewPoliciesScreenPreview() {
    ReviewPoliciesScreen(
        viewModel = FakeBackupViewModel(
            backupState = BackupStates.PoliciesReviewing,
        )
    )
}