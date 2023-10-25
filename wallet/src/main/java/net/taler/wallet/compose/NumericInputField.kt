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

package net.taler.wallet.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NumericInputField(
    modifier: Modifier = Modifier,
    value: Long,
    onValueChange: (Long) -> Unit,
    readOnly: Boolean = true,
    label: @Composable () -> Unit,
    minValue: Long? = 0L,
    maxValue: Long? = null,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value.toString(),
        singleLine = true,
        readOnly = readOnly,
        onValueChange = {
            val dd = it.toLongOrNull() ?: 0
            onValueChange(dd)
        },
        trailingIcon = {
            Row {
                IconButton(
                    content = { Icon(Icons.Default.Remove, "add1") },
                    onClick = {
                        if (minValue != null && value - 1 >= minValue) {
                            onValueChange(value - 1)
                        }
                    },
                )
                IconButton(
                    content = { Icon(Icons.Default.Add, "add1") },
                    onClick = {
                        if (maxValue != null && value + 1 <= maxValue) {
                            onValueChange(value + 1)
                        }
                    },
                )
            }
        },
        label = label,
    )
}