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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import net.taler.anastasis.R
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.common.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSecretForm(
    modifier: Modifier = Modifier,
    name: String,
    value: String,
    expiration: Timestamp? = null,
    onSecretEdited: (
        name: String,
        value: String,
        expiration: Timestamp?,
    ) -> Unit,
) {
    val focusRequester2 = remember { FocusRequester() }

    Column(
        modifier = modifier,
    ) {
        OutlinedTextField(
            modifier = Modifier
                .padding(
                    start = LocalSpacing.current.medium,
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.small,
                ).fillMaxWidth(),
            value = name,
            maxLines = 1,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusRequester2.requestFocus() }),
            onValueChange = { onSecretEdited(it, value, expiration) },
            label = { Text(stringResource(R.string.secret_name)) },
            supportingText = { Text(stringResource(R.string.secret_unique)) },
        )

        OutlinedTextField(
            modifier = Modifier
                .focusRequester(focusRequester2)
                .padding(
                    start = LocalSpacing.current.medium,
                    end = LocalSpacing.current.medium,
                    bottom = LocalSpacing.current.small,
                ).fillMaxWidth(),
            value = value,
            onValueChange = { onSecretEdited(name, it, expiration) },
            label = { Text(stringResource(R.string.secret_text)) },
        )
    }
}