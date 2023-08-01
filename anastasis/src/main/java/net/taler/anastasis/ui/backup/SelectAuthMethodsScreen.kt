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

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.shared.Utils
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.ReducerArgs
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.dialogs.EditMethodDialog
import net.taler.anastasis.ui.reusable.components.ActionCard
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.ReducerViewModel

@Composable
fun SelectAuthMethodsScreen(
    viewModel: ReducerViewModel = hiltViewModel(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer state type")

    val authProviders = reducerState.authenticationProviders ?: emptyMap()
    val selectedMethods = reducerState.authenticationMethods ?: emptyList()

    // Get only known methods of providers with "ok" status
    val availableMethods = authProviders.flatMap { entry ->
        if (entry.value is AuthenticationProviderStatus.Ok) {
            (entry.value as AuthenticationProviderStatus.Ok).methods.map { it.type }
                .filter { it != AuthMethod.Type.Unknown }
        } else emptyList()
    }.distinct()

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
                Log.d("onMethodEdited", it.challenge)
                viewModel.reducerManager.addAuthentication(
                    ReducerArgs.AddAuthentication(it)
                )
            }
        )
    }

    WizardPage(
        title = stringResource(R.string.select_auth_methods_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager.next()
        }
    ) { scroll ->
        AuthMethods(
            nestedScrollConnection = scroll,
            availableMethods = availableMethods,
            selectedMethods = selectedMethods,
            onAddMethod = {
                methodType = it
                showEditDialog = true
            },
            onDeleteMethod = {
                viewModel.reducerManager.deleteAuthentication(it)
            },
        )
    }
}

@Composable
private fun AuthMethods(
    nestedScrollConnection: NestedScrollConnection,
    availableMethods: List<AuthMethod.Type>,
    selectedMethods: List<AuthMethod>,
    onAddMethod: (type: AuthMethod.Type) -> Unit,
    onDeleteMethod: (index: Int) -> Unit,
) {
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
                onClick = { onAddMethod(method) },
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
        Column(modifier = Modifier.padding(LocalSpacing.current.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(authMethod.type.icon, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(authMethod.instructions, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = Utils.decodeBase32(authMethod.challenge),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        }
    }
}