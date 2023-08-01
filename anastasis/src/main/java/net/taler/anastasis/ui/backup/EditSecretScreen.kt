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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.CoreSecret
import net.taler.anastasis.models.ReducerArgs
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.forms.EditSecretForm
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.viewmodels.FakeReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI
import net.taler.common.Amount
import net.taler.common.CryptoUtils
import net.taler.common.Timestamp

@Composable
fun EditSecretScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Backup
        ?: error("invalid reducer state type")

    var secretName by remember {
        mutableStateOf(reducerState.secretName ?: "")
    }
    var secretValue by remember {
        mutableStateOf(reducerState.coreSecret?.value?.let {
            CryptoUtils.decodeCrock(it).toString(Charsets.UTF_8)
        } ?: "")
    }

    WizardPage(
        title = stringResource(R.string.edit_secret_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager?.backupSecret(
                name = secretName,
                args = ReducerArgs.EnterSecret(
                    secret = ReducerArgs.EnterSecret.Secret(
                        value = CryptoUtils.encodeCrock(secretValue.toByteArray(Charsets.UTF_8)),
                        mime = "text/plain",
                    ),
                    expiration = null,
                )
            )
        },
    ) {
        EditSecretForm(
            modifier = Modifier.fillMaxSize(),
            name = secretName,
            value = secretValue,
        ) { name, value, _ ->
            secretName = name
            secretValue = value
        }
    }
}

@Preview
@Composable
fun EditSecretScreenPreview() {
    EditSecretScreen(
        viewModel = FakeReducerViewModel(
            state = ReducerState.Backup(
                backupState = BackupStates.SecretEditing,
                secretName = "_TALERWALLET_MyPinePhone",
                coreSecret = CoreSecret(
                    value = "EDJP6WK5EG50",
                    mime = "text/plain",
                ),
                expiration = Timestamp.never(),
                uploadFees = listOf(
                    ReducerState.Backup.UploadFee(
                        fee = Amount("KUDOS", 42L, 0),
                    ),
                ),
            ),
        ),
    )
}