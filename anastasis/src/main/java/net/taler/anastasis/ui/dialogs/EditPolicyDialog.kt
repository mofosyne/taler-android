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

package net.taler.anastasis.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.taler.anastasis.R
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.Policy
import net.taler.anastasis.ui.forms.EditPolicyForm

@Composable
fun EditPolicyDialog(
    policy: Policy? = null,
    methods: List<AuthMethod>,
    providers: Map<String, AuthenticationProviderStatus.Ok>,
    onPolicyEdited: (policy: Policy) -> Unit,
    onCancel: () -> Unit,
) {
    var localPolicy by remember { mutableStateOf(policy) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(if (policy != null)
            R.string.edit_policy else R.string.add_policy)) },
        text = {
            EditPolicyForm(
                modifier = Modifier.fillMaxWidth(),
                policy = localPolicy,
                methods = methods,
                providers = providers,
                onPolicyEdited = {
                    localPolicy = it
                }
            )
        },
        dismissButton = {
            TextButton(onClick = {
                onCancel()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                localPolicy?.let { onPolicyEdited(it) }
            }) {
                Text(stringResource(if (policy != null)
                    R.string.edit else R.string.add))
            }
        }
    )

}
