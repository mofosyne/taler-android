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

package net.taler.anastasis.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.toLocalDate
import net.taler.anastasis.R
import net.taler.anastasis.shared.Utils
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.models.UserAttributeSpec
import net.taler.anastasis.shared.FieldStatus
import net.taler.anastasis.ui.reusable.components.DatePickerField
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.ReducerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectUserAttributesScreen(
    viewModel: ReducerViewModel = hiltViewModel(),
) {
    val reducerState by viewModel.reducerState.collectAsState()
    val userAttributes = when (val state = reducerState) {
        is ReducerState.Backup -> state.requiredAttributes
        is ReducerState.Recovery -> state.requiredAttributes
        else -> error("invalid reducer state type")
    } ?: emptyList()

    val identityAttributes = when(val state = reducerState) {
        is ReducerState.Backup -> state.identityAttributes
        is ReducerState.Recovery -> state.identityAttributes
        else -> error("invalid reducer state type")
    } ?: emptyMap()

    val values = remember { mutableStateMapOf(
        *identityAttributes.toList().toTypedArray()
    ) }

    val enableNext = remember(userAttributes, values) {
        userAttributes.fold(true) { a, b ->
            a && (fieldStatus(b, values[b.name]) == FieldStatus.Valid)
        }
    }

    WizardPage(
        title = stringResource(R.string.select_user_attributes_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager.enterUserAttributes(values)
        },
        enableNext = enableNext,
    ) { scrollConnection ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollConnection),
            verticalArrangement = Arrangement.Top,
        ) {
            items(items = userAttributes) { attr ->
                val status = remember(attr, values) {
                    fieldStatus(attr, values[attr.name])
                }
                val supportingRes = remember(attr, status) {
                    status.msgRes ?: if (attr.optional == true) {
                        R.string.field_optional
                    } else null
                }
                when (attr.type) {
                    "string" -> OutlinedTextField(
                        modifier = Modifier
                            .padding(
                                start = LocalSpacing.current.medium,
                                end = LocalSpacing.current.medium,
                            )
                            .fillMaxWidth(),
                        value = values[attr.name] ?: "",
                        onValueChange = { values[attr.name] = it },
                        isError = status.error,
                        supportingText = {
                            supportingRes?.let { Text(stringResource(it)) }
                        },
                        label = { Text(attr.label) },
                    )
                    "date" -> DatePickerField(
                        modifier = Modifier
                            .padding(
                                start = LocalSpacing.current.medium,
                                end = LocalSpacing.current.medium,
                            )
                            .fillMaxWidth(),
                        label = attr.label,
                        isError = status.error,
                        supportingText = {
                            supportingRes?.let { Text(stringResource(it)) }
                        },
                        date = values[attr.name]?.toLocalDate(),
                        onDateSelected = { date ->
                            values[attr.name] = Utils.formatDate(date)
                        },
                    )
                }
                Spacer(Modifier.height(LocalSpacing.current.small))
            }
        }
    }
}

private fun fieldStatus(
    field: UserAttributeSpec,
    value: String? = null,
): FieldStatus = if (value == null) {
    FieldStatus.Null
} else {
    if (value.isNotBlank()) {
        field.validationRegex?.toRegex()?.let {
            if (value.matches(it))
                FieldStatus.Valid else FieldStatus.Invalid
        } ?: FieldStatus.Valid
    } else if (field.optional == true)
        FieldStatus.Valid else FieldStatus.Blank
}