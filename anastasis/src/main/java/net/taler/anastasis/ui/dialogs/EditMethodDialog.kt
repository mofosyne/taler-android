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

package net.taler.anastasis.ui.dialogs

import android.util.Patterns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.ui.forms.EditAnswerForm
import net.taler.anastasis.ui.forms.EditQuestionForm

@Composable
fun EditMethodDialog(
    type: AuthMethod.Type? = null,
    method: AuthMethod? = null,
    onMethodEdited: (method: AuthMethod) -> Unit,
    onCancel: () -> Unit,
) {
    var localMethod by remember { mutableStateOf(method) }
    var valid by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.add_challenge)) },
        text = {
               when (type ?: method?.type) {
                   AuthMethod.Type.Question -> EditQuestionForm(
                       question = localMethod?.instructions,
                       answer = localMethod?.challenge,
                       onMethodEdited = { question, answer ->
                           valid = true
                           localMethod = AuthMethod(
                               type = AuthMethod.Type.Question,
                               instructions = question,
                               challenge = answer,
                               mimeType = "text/plain",
                           )
                       }
                   )
                   AuthMethod.Type.Sms -> EditAnswerForm(
                       answerLabel = stringResource(R.string.sms),
                       answer = localMethod?.challenge ?: "",
                       onAnswerEdited = { answer, v ->
                           localMethod = AuthMethod(
                               type = AuthMethod.Type.Sms,
                               instructions = context.getString(R.string.auth_instruction_sms, answer),
                               challenge = answer,
                               mimeType = "text/plain",
                           )
                           valid = v
                       },
                       keyboardType = KeyboardType.Phone,
                       regex = Patterns.PHONE.pattern(),
                   )
                   AuthMethod.Type.Email -> EditAnswerForm(
                       answerLabel = stringResource(R.string.email),
                       answer = localMethod?.challenge ?: "",
                       onAnswerEdited = { answer, v ->
                           localMethod = AuthMethod(
                               type = AuthMethod.Type.Email,
                               instructions = context.getString(R.string.auth_instruction_email, answer),
                               challenge = answer,
                               mimeType = "text/plain",
                           )
                           valid = v
                       },
                       keyboardType = KeyboardType.Email,
                       regex = Patterns.EMAIL_ADDRESS.pattern(),
                   )
                   else -> {}
               }
        },
        dismissButton = {
            TextButton(onClick = {
                onCancel()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    localMethod?.let { onMethodEdited(it) }
                },
                enabled = valid,
            ) {
                Text(stringResource(R.string.add))
            }
        }
    )
}
