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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Token
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.Utils
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.ReducerArgs
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.components.ActionCard
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.ReducerViewModel

@Composable
fun SelectAuthMethodsScreen(
    viewModel: ReducerViewModel = hiltViewModel(),
) {
    val reducerState by viewModel.reducerState.collectAsState()

    val authProviders = when (val state = reducerState) {
        is ReducerState.Backup -> state.authenticationProviders
        else -> error("invalid reducer state type")
    } ?: emptyMap()

    // Get only methods of providers with "ok" status
    val availableMethods = authProviders.flatMap { entry ->
        if (entry.value is AuthenticationProviderStatus.Ok) {
            (entry.value as AuthenticationProviderStatus.Ok).methods.map { it.type }
        } else emptyList()
    }.distinct()

    val selectedMethods = when (val state = reducerState) {
        is ReducerState.Backup -> state.authenticationMethods
        else -> error("invalid reducer state type")
    } ?: emptyList()

    var showEditDialog by remember { mutableStateOf(false) }
    var methodType by remember { mutableStateOf<String?>(null) }
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
        onBackClicked = {
            viewModel.goHome()
        },
        onPrevClicked = {
            viewModel.reducerManager.back()
        },
    ) {
        AuthMethods(
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
    availableMethods: List<String>,
    selectedMethods: List<AuthMethod>,
    onAddMethod: (type: String) -> Unit,
    onDeleteMethod: (index: Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .padding(LocalSpacing.current.medium)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
    ) {
        items(items = availableMethods) { method ->
            when (method) {
                "question" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.QuestionMark, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_question)
                ) { onAddMethod("question") }
                "sms" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.Sms, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_sms)
                ) { onAddMethod("sms") }
                "email" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_email)
                ) { onAddMethod("email") }
                "iban" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_question)
                ) { onAddMethod("iban") }
                "mail" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.Mail, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_mail)
                ) { onAddMethod("mail") }
                "totp" -> ActionCard(
                    modifier = Modifier
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                    icon = { Icon(Icons.Default.Token, contentDescription = null) },
                    headline = stringResource(R.string.auth_method_totp)
                ) { onAddMethod("totp") }
                else -> {}
            }
        }

        item {
            Divider(Modifier.padding(bottom = LocalSpacing.current.small))
        }

        items(count = selectedMethods.size) { i ->
            ChallengeCard(
                modifier = Modifier
                    .padding(bottom = LocalSpacing.current.small)
                    .fillMaxWidth(),
                authMethod = selectedMethods[i],
                onDelete = { onDeleteMethod(i) },
            )
        }
    }
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
                Icon(when (authMethod.type) {
                    "question" -> Icons.Default.QuestionMark
                    "sms" -> Icons.Default.Sms
                    "email" -> Icons.Default.Email
                    "iban" -> Icons.Default.AccountBalance
                    "mail" -> Icons.Default.Mail
                    "totp" -> Icons.Default.Token
                    else -> error("unknown auth method")
                }, contentDescription = null)
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