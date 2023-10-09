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

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
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
import net.taler.anastasis.ui.components.PinInputField
import net.taler.anastasis.ui.components.QrCode
import net.taler.anastasis.ui.theme.AnastasisTheme
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.common.CryptoUtils
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUnsignedTypes::class)
@Composable
fun EditTotpForm(
    modifier: Modifier = Modifier,
    secret: UByteArray,
    name: String,
    code: String,
    digits: Int = 8,
    onTotpEdited: (
        name: String,
        code: String,
        valid: Boolean,
    ) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val nameStatus = remember(name) { nameFieldStatus(name) }
    val codeStatus = remember(code) { codeFieldStatus(code, secret, digits) }

    Column(modifier) {
        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth(),
            value = name,
            isError = nameStatus.error,
            supportingText = {
                nameStatus.msgRes?.let { Text(stringResource(it)) }
            },
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            onValueChange = {
                onTotpEdited(it, code,
                    nameFieldStatus(it) == FieldStatus.Valid
                            && codeStatus == FieldStatus.Valid)
            },
            label = { Text(stringResource(R.string.totp_name)) },
        )

        val uri = getTotpUri(name, digits, CryptoUtils.encodeBase32(secret))

        QrCode(
            uri = uri,
            clipBoardLabel = "otp"
        ) {
            Text(stringResource(R.string.totp_instructions))
        }

        PinInputField(
            modifier = Modifier
                .padding(top = LocalSpacing.current.small).fillMaxWidth(),
            pinSize = 8,
            pin = code,
            onPinChanged = {
                onTotpEdited(name, it,
                    nameStatus == FieldStatus.Valid
                            && codeFieldStatus(it, secret, digits) == FieldStatus.Valid)
            },
            label = { Text(stringResource(R.string.code_totp)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

fun nameFieldStatus(name: String): FieldStatus = if (name.isBlank()) {
    FieldStatus.Blank
} else {
    FieldStatus.Valid
}

@OptIn(ExperimentalUnsignedTypes::class)
fun codeFieldStatus(code: String, secret: UByteArray, digits: Int): FieldStatus =
    if (code.isBlank()) {
        FieldStatus.Blank
    } else if (code.toIntOrNull()?.let {
            CryptoUtils.computeTotpAndCheck(
                secretKey = secret,
                digits = digits,
                code = it,
            )
        } == true) {
        FieldStatus.Valid
    } else {
        FieldStatus.Invalid
    }

fun getTotpUri(name: String, digits: Int, secret: String) =
    "otpauth://totp/${Uri.encode(name)}?digits=$digits&secret=$secret"

@OptIn(ExperimentalUnsignedTypes::class)
@Preview
@Composable
fun EditTotpFormPreview() {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val secret = remember { Random.nextBytes(32).toUByteArray() }
    AnastasisTheme {
        Surface {
            EditTotpForm(
                name = name,
                code = code,
                secret = secret,
                onTotpEdited = { n, c, _ ->
                    name = n
                    code = c
                }
            )
        }
    }
}