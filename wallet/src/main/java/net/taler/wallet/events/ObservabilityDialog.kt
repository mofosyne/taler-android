/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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

package net.taler.wallet.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.copyToClipBoard
import net.taler.wallet.events.ObservabilityDialog.Companion.json

class ObservabilityDialog: DialogFragment() {
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val events by model.observabilityLog.observeAsState()
            ObservabilityComposable(events?.reversed() ?: emptyList()) {
                dismiss()
            }
        }
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val json = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
    }
}

@Composable
fun ObservabilityComposable(
    events: List<ObservabilityEvent>,
    onDismiss: () -> Unit,
) {
    var showJson by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(stringResource(R.string.observability_title)) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events) { event ->
                    ObservabilityItem(event, showJson)
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            Button(onClick = { showJson = !showJson }) {
                Text(if (showJson) {
                    stringResource(R.string.observability_hide_json)
                } else {
                    stringResource(R.string.observability_show_json)
                })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
fun ObservabilityItem(
    event: ObservabilityEvent,
    showJson: Boolean,
) {
    val context = LocalContext.current
    val title = event.getTitle(context)
    val body = json.encodeToString(event.body)

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = { Text(title) },
        supportingContent = if (!showJson) null else { ->
            Text(
                text = body,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = if(!showJson) null else { ->
            IconButton(onClick = {
                copyToClipBoard(context, "Event", body)
            }) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.copy),
                )
            }
        },
    )
}