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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import net.taler.anastasis.R
import net.taler.anastasis.Utils
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.ui.forms.EditQuestionForm

@Composable
fun EditMethodDialog(
    type: AuthMethod.Type? = null,
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
                   AuthMethod.Type.Question -> EditQuestionForm(
                       method = localMethod,
                       onMethodEdited = {
                           localMethod = it
                       },
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
