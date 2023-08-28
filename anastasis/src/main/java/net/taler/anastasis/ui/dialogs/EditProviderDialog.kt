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
import androidx.compose.ui.res.stringResource
import net.taler.anastasis.R
import net.taler.anastasis.ui.forms.EditAnswerForm

@Composable
fun EditProviderDialog(
    onProviderEdited: (url: String) -> Unit,
    onCancel: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var valid by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.add_provider)) },
        text = {
               EditAnswerForm(
                   answerLabel = stringResource(R.string.url),
                   onAnswerEdited = { answer, v ->
                       url = answer
                       valid = v
                   },
                   answer = url,
                   regex = Patterns.WEB_URL.pattern(),
               )
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
                    onProviderEdited(url)
                },
                enabled = valid,
            ) {
                Text(stringResource(R.string.add))
            }
        }
    )
}