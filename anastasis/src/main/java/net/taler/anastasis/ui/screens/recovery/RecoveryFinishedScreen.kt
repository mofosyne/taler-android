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

package net.taler.anastasis.ui.screens.recovery

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeRecoveryViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI
import net.taler.common.CryptoUtils
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecoveryFinishedScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val state by viewModel.reducerState.collectAsState()
    val reducerState = state as? ReducerState.Recovery
        ?: error("invalid reducer state type")

    val recoveryDocument = reducerState.recoveryDocument!!
    val coreSecret = reducerState.coreSecret!!

    WizardPage(
        title = stringResource(R.string.recovery_finished_title),
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
                    recoveryDocument.secretName,
                    modifier = Modifier.padding(LocalSpacing.current.medium),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            item {
                if (coreSecret.value.length < 1000 && coreSecret.mime == "plain/text") {
                    OutlinedTextField(
                        modifier = Modifier
                            .padding(
                                start = LocalSpacing.current.medium,
                                end = LocalSpacing.current.medium,
                                bottom = LocalSpacing.current.small,
                            )
                            .fillMaxWidth(),
                        value = CryptoUtils.decodeCrock(coreSecret.value).toString(Charsets.UTF_8),
                        readOnly = true,
                        onValueChange = {},
                        label = { Text(stringResource(R.string.secret_text)) },
                    )
                } else {
                    val context = LocalContext.current
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument(coreSecret.mime ?: "application/octet-stream"),
                        onResult = { uri ->
                            if (uri != null) {
                                val outputStream = context.contentResolver.openOutputStream(uri)
                                if (outputStream != null) {
                                    try {
                                        // TODO: we should probably listen to Java and not block
                                        outputStream.write(CryptoUtils.decodeCrock(coreSecret.value))
                                        outputStream.flush()
                                        outputStream.close()
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.recovery_file_saved),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    )

                    OutlinedButton(onClick = {
                        launcher.launch(coreSecret.filename ?: UUID.randomUUID().toString())
                    }) {
                        Text(stringResource(R.string.recovery_download_file))
                    }
                    Log.d("RecoveryFinishedScreen", "data = ${coreSecret.value}")
                    Log.d("RecoveryFinishedScreen", "filename = ${coreSecret.filename}")
                    Log.d("RecoveryFinishedScreen", "mimeType = ${coreSecret.mime}")
                }
            }
        }
    }
}

@Preview
@Composable
fun RecoveryFinishedScreenPreview() {
    RecoveryFinishedScreen(
        viewModel = FakeRecoveryViewModel(
            recoveryState = RecoveryStates.RecoveryFinished,
        )
    )
}