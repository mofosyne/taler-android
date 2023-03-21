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

package net.taler.wallet.transactions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorInfo
import net.taler.wallet.compose.copyToClipBoard

@Composable
fun ErrorTransactionButton(
    modifier: Modifier = Modifier,
    error: TalerErrorInfo,
) {
    val showDialog = remember { mutableStateOf(false) }

    if (showDialog.value) {
        @Suppress("OPT_IN_USAGE")
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        val message = json.encodeToString(error)
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = {
                Text(stringResource(R.string.nav_error))
            },
            text = {
                Column {
                    Text(
                        fontFamily = FontFamily.Monospace,
                        text = message,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog.value = false
                }) {
                    Text(stringResource(R.string.close))
                }
            },
            confirmButton = {
                val context = LocalContext.current
                TextButton(onClick = {
                    copyToClipBoard(context, context.getString(R.string.nav_error), message)
                }) {
                    Text(stringResource(R.string.copy))
                }
            })
    }

    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            contentColor = MaterialTheme.colorScheme.onError,
            containerColor = MaterialTheme.colorScheme.error,
        ),
        onClick = {
            showDialog.value = true
        }
    ) {
        val label = stringResource(R.string.nav_error)
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = label,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(label)
    }
}
