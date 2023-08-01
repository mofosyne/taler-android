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

package net.taler.anastasis.ui.forms

import android.util.Patterns
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.shared.FieldStatus


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSmsForm(
    method: AuthMethod? = null,
    onMethodEdited: (method: AuthMethod) -> Unit,
) {
    val localMethod = method ?: AuthMethod(
        type = AuthMethod.Type.Sms,
        instructions = stringResource(R.string.auth_instruction_sms, ""),
        challenge = "",
        mimeType = "text/plain",
    )

    val context = LocalContext.current
    val focusRequester1 = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val status = fieldStatus(localMethod.challenge)

    OutlinedTextField(
        modifier = Modifier
            .focusRequester(focusRequester1)
            .fillMaxWidth(),
        value = localMethod.challenge,
        isError = status.error,
        supportingText = {
            status.msgRes?.let { Text(stringResource(it)) }
        },
        maxLines = 1,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        onValueChange = {
            onMethodEdited(localMethod.copy(
                instructions = context.getString(R.string.auth_instruction_sms, it),
                challenge = it,
            ))
        },
        label = { Text(stringResource(R.string.sms)) },
    )

    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
}

private fun fieldStatus(phone: String): FieldStatus = if (phone.isBlank()) {
    FieldStatus.Blank
} else if (Patterns.PHONE.matcher(phone).matches()) {
    FieldStatus.Valid
} else {
    FieldStatus.Invalid
}