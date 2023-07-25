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

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.LocalDate
import net.taler.anastasis.Utils
import net.taler.anastasis.ui.theme.LocalSpacing
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    yy: Int,
    mm: Int,
    dd: Int,
    onDateSelected: (yy: Int, mm: Int, dd: Int) -> Unit,
) {
    val dialog = DatePickerDialog(
        LocalContext.current,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            onDateSelected(y, m, d)
        }, yy, mm, dd,
    )

    OutlinedTextField(
        modifier = modifier,
        value = Utils.formatDate(LocalDate(yy, mm, dd)),
        label = { Text(label) },
        onValueChange = { },
        trailingIcon = {
            Button(
                modifier = Modifier.padding(end = LocalSpacing.current.medium),
                onClick = { dialog.show() },
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                )
            }
        },
        textStyle = LocalTextStyle.current.copy( // show text as if not disabled
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        enabled = false,
        readOnly = true,
    )
}

@Preview
@Composable
fun DatePickerFieldPreview() {
    val cal = Calendar.getInstance()
    var yy by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var mm by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var dd by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    Surface {
        DatePickerField(
            label = "Birthdate",
            yy = yy,
            mm = mm,
            dd = dd,
            onDateSelected = { y, m, d ->
                yy = y
                mm = m
                dd = d
            },
        )
    }
}