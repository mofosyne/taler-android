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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.PopupProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownTextField(
    modifier: Modifier = Modifier,
    label: String,
    options: Set<String>,
    onOptionChanged: (String) -> Unit,
) {
    var filteredOptions by remember { mutableStateOf(options.toList()) }
    var inputValue by remember { mutableStateOf(options.first()) }
    var expanded by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(Size.Zero) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.Center)
            .focusRequester(focusRequester),
    ) {
        OutlinedTextField(
            modifier = modifier
                .onGloballyPositioned { coordinates ->
                    size = coordinates.size.toSize()
                },
            value = inputValue,
            onValueChange = { value ->
                inputValue = value
                expanded = true
                filteredOptions = options.filter { it.contains(value) }
            },
            singleLine = true,
            label = { Text(label) },
            trailingIcon = {
                Box(modifier = Modifier.clickable { expanded = !expanded }) {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded,
                    )
                }
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )

        if (filteredOptions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                /*
                 * TODO: we should NOT disable focus, but this will be necessary
                 *  until Google fixes ExposedDropdownMenuBox focus crash.
                 */
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .width(with(LocalDensity.current) { size.width.toDp() }),
            ) {
                filteredOptions.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Text(text = s)
                        },
                        onClick = {
                            inputValue = s
                            expanded = false
                            onOptionChanged(s)
                        }
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun DropdownTextFieldComposable() {
    Surface {
        DropdownTextField(
            label = "Continent",
            options = setOf("Europe", "India", "Asia", "North America"),
            onOptionChanged = {},
        )
    }
}