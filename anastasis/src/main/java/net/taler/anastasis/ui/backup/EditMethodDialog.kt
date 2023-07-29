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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.taler.anastasis.R
import net.taler.anastasis.Utils
import net.taler.anastasis.models.AuthMethod

@Composable
fun EditMethodDialog(
    type: String? = null,
    method: AuthMethod? = null,
    onMethodEdited: (method: AuthMethod) -> Unit,
    onCancel: () -> Unit,
) {
    var localMethod by remember { mutableStateOf(method?.copy(
        challenge = Utils.decodeBase32(method.challenge),
    )) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.add_challenge)) },
        text = {
               when(type ?: method?.type) {
                   "question" -> EditQuestionForm(
                       method = localMethod,
                       onMethodEdited = {
                           localMethod = it
                       },
                   )
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
            TextButton(onClick = {
                localMethod?.let { onMethodEdited(
                    it.copy(
                        challenge = Utils.encodeBase32(it.challenge)
                    )
                ) }
            }) {
                Text(stringResource(R.string.add))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditQuestionForm(
    method: AuthMethod? = null,
    onMethodEdited: (method: AuthMethod) -> Unit,
) {
    val localMethod = method ?: AuthMethod(
        type = "question",
        instructions = "",
        challenge = "",
        mimeType = "plain/text",
    )

    Column {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = localMethod.instructions,
            onValueChange = {
                onMethodEdited(localMethod.copy(instructions = it))
            },
            label = { Text(stringResource(R.string.question)) },
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = localMethod.challenge,
            onValueChange = {
                onMethodEdited(localMethod.copy(challenge = it))
            },
            label = { Text(stringResource(R.string.answer)) },
        )
    }

}