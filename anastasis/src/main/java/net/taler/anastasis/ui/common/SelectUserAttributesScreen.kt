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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.components.DatePickerField
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.ReducerViewModel
import java.util.Calendar

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

    val values = remember { mutableStateMapOf<String, String>() }

    WizardPage(
        title = stringResource(R.string.select_user_attributes_title),
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            viewModel.reducerManager.enterUserAttributes(values)
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(it)
                .padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.Top,
        ) {
            items(items = userAttributes) { attr ->
                when (attr.type) {
                    "string" -> OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = values[attr.name] ?: "",
                        onValueChange = { values[attr.name] = it },
                        label = { Text(attr.label) },
                    )
                    "date" -> @Composable {
                        val cal = Calendar.getInstance()
                        var yy by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
                        var mm by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
                        var dd by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
                        DatePickerField(
                            modifier = Modifier.fillMaxWidth(),
                            label = attr.label,
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
                Spacer(Modifier.height(LocalSpacing.current.small))
            }
        }
    }
}