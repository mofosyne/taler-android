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

package net.taler.anastasis.ui.forms

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.taler.anastasis.R
import net.taler.anastasis.shared.Utils.currentDate
import net.taler.anastasis.ui.components.DatePickerField
import net.taler.anastasis.ui.theme.AnastasisTheme
import net.taler.anastasis.ui.theme.LocalSpacing

enum class SecretType { PlainText, File }

sealed class SecretData {
    object Empty: SecretData()

    class PlainText(val value: String): SecretData()

    class File(val documentUri: Uri): SecretData()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSecretForm(
    modifier: Modifier = Modifier,
    name: String,
    data: SecretData,
    expirationDate: LocalDate,
    onSecretNameEdited: (name: String) -> Unit,
    onSecretEdited: (data: SecretData) -> Unit,
    onExpirationEdited: (expirationDate: LocalDate) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .padding(
                    start = LocalSpacing.current.medium,
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.small,
                )
                .fillMaxWidth(),
            value = name,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
            onValueChange = { onSecretNameEdited(it) },
            label = { Text(stringResource(R.string.secret_name)) },
            supportingText = { Text(stringResource(R.string.secret_unique)) },
        )

        var selectedType by remember(data) { mutableStateOf(when(data) {
            is SecretData.Empty -> SecretType.PlainText
            is SecretData.PlainText -> SecretType.PlainText
            is SecretData.File -> SecretType.File
        }) }

        Column(
            modifier = Modifier
                .padding(
                    top = LocalSpacing.current.small,
                    end = LocalSpacing.current.medium,
                    start = LocalSpacing.current.medium,
                )
                .selectableGroup(),
        ) {
            SecretType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .selectable(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            role = Role.RadioButton,
                        )
                        .padding(bottom = LocalSpacing.current.small)
                        .fillMaxWidth(),
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = null,
                    )
                    Text(
                        text = when (type) {
                            SecretType.PlainText -> stringResource(R.string.secret_type_plain_text)
                            SecretType.File -> stringResource(R.string.secret_type_file)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = LocalSpacing.current.medium),
                    )
                }
            }
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = {
                it?.let { uri -> onSecretEdited(SecretData.File(uri)) }
            }
        )

        val text = (data as? SecretData.PlainText)?.value ?: ""
        when (selectedType) {
            SecretType.PlainText -> OutlinedTextField(
                modifier = Modifier
                    .padding(
                        start = LocalSpacing.current.medium,
                        end = LocalSpacing.current.medium,
                        bottom = LocalSpacing.current.small,
                    )
                    .fillMaxWidth(),
                value = text,
                onValueChange = {
                    onSecretEdited(SecretData.PlainText(it))
                },
                label = { Text(stringResource(R.string.secret_text)) },
            )
            SecretType.File -> OutlinedButton(
                modifier = Modifier.padding(
                    start = LocalSpacing.current.medium,
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.small,
                ),
                onClick = { launcher.launch("*/*") }
            ) {
                Text(
                    if (data is SecretData.File)
                        stringResource(R.string.secret_file_chosen)
                    else
                        stringResource(R.string.secret_choose_file),
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        DatePickerField(
            modifier = Modifier
                .padding(
                    start = LocalSpacing.current.medium,
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.small,
                ).fillMaxWidth(),
            label = stringResource(R.string.secret_expiration),
            date = expirationDate,
            onDateSelected = { onExpirationEdited(it) },
            minDate = currentDate,
        )
    }
}

@Preview
@Composable
fun EditSecretFormPreview() {
    var name by remember { mutableStateOf("") }
    var data by remember { mutableStateOf<SecretData>(SecretData.Empty) }
    var expirationDate by remember { mutableStateOf(
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ) }
    AnastasisTheme {
        Surface {
            EditSecretForm(
                name = name,
                data = data,
                expirationDate = expirationDate,
                onSecretNameEdited = { name = it },
                onSecretEdited = { data = it },
                onExpirationEdited = { expirationDate = it },
            )
        }
    }
}