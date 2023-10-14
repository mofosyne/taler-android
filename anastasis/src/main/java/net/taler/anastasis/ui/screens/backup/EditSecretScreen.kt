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

package net.taler.anastasis.ui.screens.backup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import net.taler.anastasis.R
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.CoreSecret
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.shared.FileUtils.resolveDocFilename
import net.taler.anastasis.shared.FileUtils.resolveDocMimeType
import net.taler.anastasis.ui.forms.EditSecretForm
import net.taler.anastasis.ui.forms.SecretData
import net.taler.anastasis.ui.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeBackupViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI
import net.taler.common.Amount
import net.taler.common.CryptoUtils
import net.taler.common.Timestamp
import kotlin.time.Duration.Companion.days

@Composable
fun EditSecretScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer state type")
    val coreSecret = reducerState.coreSecret

    val tz = TimeZone.currentSystemDefault()
    val secretExpirationDate = remember(reducerState.expiration) {
        reducerState.expiration?.ms?.let {
            Instant.fromEpochMilliseconds(it)
        }?.toLocalDateTime(tz) ?: (Clock.System.now() + 365.days).toLocalDateTime(tz)
    }
    val uploadFees = reducerState.uploadFees ?: emptyList()

    var secretName by remember { mutableStateOf(reducerState.secretName ?: "") }
    var secretData by remember { mutableStateOf<SecretData>(SecretData.Empty) }

    WizardPage(
        title = stringResource(R.string.edit_secret_title),
        onBackClicked = { viewModel.goHome() },
        enableNext = secretName.isNotEmpty() && coreSecret != null,
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager?.next()
        },
    ) { scroll ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scroll),
        ) {
            item {
                val context = LocalContext.current
                EditSecretForm(
                    modifier = Modifier.fillMaxSize(),
                    name = secretName,
                    data = secretData,
                    expirationDate = secretExpirationDate.date,
                    coreSecret = coreSecret,
                    onSecretNameEdited = { name ->
                        secretName = name
                        viewModel.reducerManager?.enterSecretName(name)
                    },
                    onSecretEdited = { data ->
                        secretData = data
                        if (data !is SecretData.Empty) {
                            val expiration = Timestamp.fromMillis(
                                secretExpirationDate
                                    .toInstant(tz)
                                    .toEpochMilliseconds()
                            )
                            when (data) {
                                is SecretData.File -> {
                                    val filename = context.resolveDocFilename(data.documentUri)
                                    val mimeType = context.resolveDocMimeType(data.documentUri)
                                    val inputStream = context.contentResolver.openInputStream(data.documentUri)
                                    viewModel.reducerManager?.enterFileSecret(
                                        inputStream = inputStream,
                                        secret = CoreSecret(
                                            value = "", // Doesn't matter
                                            filename = filename,
                                            mime = mimeType,
                                        ),
                                        expiration = expiration,
                                    )
                                }
                                is SecretData.PlainText -> {
                                    viewModel.reducerManager?.enterSecret(
                                        secret = CoreSecret(
                                            value = CryptoUtils.encodeCrock(data.value.toByteArray(Charsets.UTF_8)),
                                            mime = "text/plain",
                                        ),
                                        expiration = expiration,
                                    )
                                }
                                else -> {} // Impossible case
                            }
                        }
                    },
                    onExpirationEdited = { date ->
                        viewModel.reducerManager?.updateSecretExpiration(
                            expiration = Timestamp.fromMillis(
                                date.atTime(secretExpirationDate.time)
                                    .toInstant(tz)
                                    .toEpochMilliseconds()
                            )
                        )
                    }
                )
            }

            item {
                if (uploadFees.isNotEmpty()) {
                    Text(
                        stringResource(R.string.secret_backup_fees),
                        modifier = Modifier.padding(
                            start = LocalSpacing.current.medium,
                            top = LocalSpacing.current.small,
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            items(items = uploadFees) { fee ->
                FeeCard(
                    modifier = Modifier
                        .padding(
                            start = LocalSpacing.current.medium,
                            end = LocalSpacing.current.medium,
                            bottom = LocalSpacing.current.small,
                        )
                        .fillMaxSize(),
                    fee = fee.fee,
                )
            }
        }
    }
}

@Composable
fun FeeCard(
    modifier: Modifier = Modifier,
    fee: Amount,
) {
    ElevatedCard(modifier) {
        Text(
            fee.toString(),
            modifier = Modifier.padding(LocalSpacing.current.medium)
        )
    }
}

@Preview
@Composable
fun EditSecretScreenPreview() {
    EditSecretScreen(
        viewModel = FakeBackupViewModel(
            backupState = BackupStates.SecretEditing,
        )
    )
}