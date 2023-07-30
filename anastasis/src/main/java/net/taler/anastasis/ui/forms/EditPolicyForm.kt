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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.Policy
import net.taler.anastasis.ui.reusable.components.DropdownTextField
import net.taler.anastasis.ui.theme.LocalSpacing

@Composable
fun EditPolicyForm(
    modifier: Modifier = Modifier,
    policy: Policy?,
    methods: List<AuthMethod>,
    providers: Map<String, AuthenticationProviderStatus.Ok>,
    onPolicyEdited: (policy: Policy) -> Unit,
) {
    val localPolicy = policy ?: Policy(methods = listOf())
    val localMethods = localPolicy.methods.associateBy { it.authenticationMethod }
    val submitLocalMethods = { it: Map<Int, Policy.PolicyMethod> ->
        onPolicyEdited(
            localPolicy.copy(
                methods = it.flatMap { entry ->
                    listOf(entry.value)
                }
            )
        )
    }

    LazyColumn(
        modifier = modifier,
    ) {
        items(count = methods.size) { index ->
            val method = methods[index]
            // Get only the providers that support this method type
            val methodProviders = providers.filterValues { provider ->
                method.type in provider.methods.map { it.type }
            }.keys.toList()
            val selectedProvider = localMethods[index]?.provider
            val checked = selectedProvider != null
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    enabled = checked,
                    checked = checked,
                    onCheckedChange = {
                        if (it) selectedProvider?.let { prov ->
                            submitLocalMethods(
                                localMethods.toMutableMap().apply {
                                    this[index] = Policy.PolicyMethod(
                                        authenticationMethod = index,
                                        provider = prov,
                                    )
                                }
                            )
                        } else {
                            submitLocalMethods(
                                localMethods.toMutableMap().apply {
                                    remove(index)
                                }
                            )
                        }
                    },
                )
                DropdownTextField(
                    modifier = Modifier.padding(bottom = LocalSpacing.current.small),
                    label = method.instructions,
                    leadingIcon = {
                        Icon(
                            method.type.icon,
                            contentDescription = stringResource(method.type.nameRes),
                        )
                    },
                    selectedIndex = selectedProvider?.let{ methodProviders.indexOf(it) },
                    options = methodProviders,
                    onOptionSelected = {
                        submitLocalMethods(
                            localMethods.toMutableMap().apply {
                                this[index] = Policy.PolicyMethod(
                                    authenticationMethod = index,
                                    provider = methodProviders[it],
                                )
                            }
                        )
                    },
                )
            }
        }
    }
}