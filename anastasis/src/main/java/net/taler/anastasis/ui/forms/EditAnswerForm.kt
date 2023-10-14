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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import net.taler.anastasis.R
import net.taler.anastasis.shared.FieldStatus
import net.taler.anastasis.ui.theme.AnastasisTheme

@Composable
fun EditAnswerForm(
    questionLabel: String? = null,
    question: String? = null,
    answerLabel: String,
    answer: String = "",
    onAnswerEdited: (answer: String, valid: Boolean) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    regex: String? = null,
) {
    val focusRequester1 = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val status = remember(answer) {
        fieldStatus(answer, regex)
    }

    Column {
        if (question != null) {
            OutlinedTextField(
                modifier = Modifier
                    .focusRequester(focusRequester1)
                    .fillMaxWidth(),
                value = question,
                maxLines = 1,
                enabled = false,
                label = { Text(questionLabel ?: stringResource(R.string.question)) },
                onValueChange = {},
            )
        }

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester1)
                .fillMaxWidth(),
            value = answer,
            isError = status.error,
            supportingText = {
                status.msgRes?.let { Text(stringResource(it)) }
            },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            onValueChange = {
                onAnswerEdited(it, fieldStatus(it, regex) == FieldStatus.Valid)
            },
            label = { Text(answerLabel) },
        )
    }

    LaunchedEffect(Unit) {
        focusRequester1.requestFocus()
    }
}

private fun fieldStatus(answer: String, regex: String? = null): FieldStatus = if (answer.isBlank()) {
    FieldStatus.Blank
} else if (regex?.toRegex()?.matches(answer) != false) {
    FieldStatus.Valid
} else {
    FieldStatus.Invalid
}

@Preview
@Composable
fun EditCodeFormPreview() {
    var code by remember { mutableStateOf("A-65611-546-7467-369") }
    AnastasisTheme {
        Surface {
            EditAnswerForm(
                answer = code,
                answerLabel = stringResource(R.string.code),
                onAnswerEdited = { answer, _ ->
                    code = answer
                },
            )
        }
    }
}