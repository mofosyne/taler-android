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

package net.taler.anastasis.ui.components

import android.app.DatePickerDialog
import android.content.Context
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import net.taler.anastasis.shared.Utils
import net.taler.anastasis.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    modifier: Modifier = Modifier,
    label: String,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    date: LocalDate?,
    onDateSelected: (date: LocalDate) -> Unit,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    val dialog = getPickerDialog(
        context = LocalContext.current,
        initialDate = date,
        minDate = minDate,
        maxDate = maxDate,
        onDateSelected = { onDateSelected(it) },
    )

    OutlinedTextField(
        modifier = modifier,
        value = date?.let { Utils.formatDate(it) } ?: "",
        label = { Text(label) },
        onValueChange = { },
        isError = isError,
        supportingText = supportingText,
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

private fun getPickerDialog(
    context: Context,
    initialDate: LocalDate?,
    minDate: LocalDate?,
    maxDate: LocalDate?,
    onDateSelected: (date: LocalDate) -> Unit,
): DatePickerDialog {
    val now = Utils.currentDate
    val tz = TimeZone.currentSystemDefault()
    val dialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int ->
            onDateSelected(LocalDate(
                year = y,
                monthNumber = m + 1,
                dayOfMonth = d,
            ))
        },
        initialDate?.year ?: now.year,
        (initialDate?.monthNumber ?: now.monthNumber) - 1,
        initialDate?.dayOfMonth ?: now.dayOfMonth,
    )
    if (minDate != null) {
        dialog.datePicker.minDate = minDate.atStartOfDayIn(tz).toEpochMilliseconds()
    }
    if (maxDate != null) {
        dialog.datePicker.maxDate = maxDate.atStartOfDayIn(tz).toEpochMilliseconds()
    }
    return dialog
}

@Preview
@Composable
fun DatePickerFieldPreview() {
    var date by remember { mutableStateOf(Utils.currentDate) }
    Surface {
        DatePickerField(
            label = "Birthdate",
            date = date,
            onDateSelected = { date = it },
        )
    }
}