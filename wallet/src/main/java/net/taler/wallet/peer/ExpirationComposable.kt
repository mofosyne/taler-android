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

package net.taler.wallet.peer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.taler.wallet.R
import net.taler.wallet.compose.NumericInputField
import net.taler.wallet.compose.SelectionChip
import net.taler.wallet.compose.TalerSurface

enum class ExpirationOption(val hours: Long) {
    DAYS_1(24),
    DAYS_7(24 * 7),
    DAYS_30(24 * 30),
    CUSTOM(-1)
}

@Composable
fun ExpirationComposable(
    modifier: Modifier = Modifier,
    option: ExpirationOption,
    hours: Long,
    onOptionChange: (ExpirationOption) -> Unit,
    onHoursChange: (Long) -> Unit,
) {
    val options = listOf(
        ExpirationOption.DAYS_1 to stringResource(R.string.send_peer_expiration_1d),
        ExpirationOption.DAYS_7 to stringResource(R.string.send_peer_expiration_7d),
        ExpirationOption.DAYS_30 to stringResource(R.string.send_peer_expiration_30d),
        ExpirationOption.CUSTOM to stringResource(R.string.send_peer_expiration_custom),
    )
    Column(
        modifier = modifier,
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items(items = options, key = { it.first }) {
                SelectionChip(
                    label = { Text(it.second) },
                    modifier = Modifier.padding(horizontal = 4.dp),
                    selected = it.first == option,
                    value = it.first,
                    onSelected = { o ->
                        onOptionChange(o)
                        if (o != ExpirationOption.CUSTOM) {
                            onHoursChange(o.hours)
                        }
                    },
                )
            }
        }

        if (option == ExpirationOption.CUSTOM) {
            val d = hours / 24L
            val h = hours - d * 24L
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                NumericInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(end = 4.dp),
                    value = d,
                    onValueChange = {
                        onHoursChange(it * 24 + h)
                    },
                    label = { Text(stringResource(R.string.send_peer_expiration_days)) },
                    maxValue = 365,
                )
                NumericInputField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 4.dp),
                    value = h,
                    onValueChange = {
                        onHoursChange(d * 24 + it)
                    },
                    label = { Text(stringResource(R.string.send_peer_expiration_hours)) },
                    maxValue = 23,
                )
            }
        }
    }
}

@Preview
@Composable
fun ExpirationComposablePreview() {
    TalerSurface {
        var option = ExpirationOption.CUSTOM
        var hours = 25L
        ExpirationComposable(
            option = option,
            hours = hours,
            onOptionChange = { option = it }
        ) { hours = it }
    }
}