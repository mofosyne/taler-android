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

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun <T> SelectionChip(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean,
    value: T,
    onSelected: (T) -> Unit,
) {
    val theme = MaterialTheme.colorScheme
    SuggestionChip(
        label = label,
        modifier = modifier,
        onClick = {
            onSelected(value)
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = if (selected) theme.primaryContainer else Color.Transparent,
            labelColor = if (selected) theme.onPrimaryContainer else theme.onSurface
        )
    )
}