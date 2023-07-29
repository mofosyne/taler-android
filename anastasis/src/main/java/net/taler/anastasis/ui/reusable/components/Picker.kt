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

package net.taler.anastasis.ui.reusable.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import net.taler.anastasis.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun Picker(
    modifier: Modifier = Modifier,
    label: String,
    options: Set<String>,
    onOptionChanged: (String) -> Unit,
) {
    var filteredOptions by remember { mutableStateOf(options.toList()) }
    var inputValue by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = modifier
            .wrapContentSize(Alignment.Center)
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = inputValue,
            onValueChange = { value ->
                inputValue = value
                filteredOptions = options.filter { it.contains(value) }
            },
            singleLine = true,
            label = { Text(label) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        if (filteredOptions.isNotEmpty()) {
            Spacer(Modifier.height(LocalSpacing.current.medium))
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items = filteredOptions) {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            inputValue = it
                            keyboardController?.hide()
                            onOptionChanged(it)
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PickerPreview() {
    Picker(
        label = "Country",
        options = setOf("Europe", "India", "Asia", "North America"),
        onOptionChanged = {},
    )
}