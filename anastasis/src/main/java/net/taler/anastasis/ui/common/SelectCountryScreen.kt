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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.components.Picker
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.ReducerViewModel

@Composable
fun SelectCountryScreen(
    viewModel: ReducerViewModel = hiltViewModel(),
) {
    val reducerState by viewModel.reducerState.collectAsState()
    val countries = when (val state = reducerState) {
        is ReducerState.Backup -> state.countries
        is ReducerState.Recovery -> state.countries
        else -> error("invalid reducer state type")
    } ?: emptyList()

    val selectedCountry = when (val state = reducerState) {
        is ReducerState.Backup -> state.selectedCountry
        is ReducerState.Recovery -> state.selectedCountry
        else -> error("invalid reducer state type")
    }

    var localCountry by remember {
        mutableStateOf(selectedCountry?.let { selected ->
            countries.find { it.code == selected }
        })
    }

    WizardPage(
        title = stringResource(R.string.select_country_title),
        enableNext = localCountry != null,
        onBackClicked = { viewModel.goHome() },
        onPrevClicked = { viewModel.goBack() },
        onNextClicked = {
            localCountry?.let {
                viewModel.reducerManager.selectCountry(it)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(LocalSpacing.current.medium),
            verticalArrangement = Arrangement.Top,
        ) {
            Picker(
                label = stringResource(R.string.country),
                initialOption = localCountry?.name,
                options = countries.map { it.name }.toSet(),
                onOptionChanged = { option ->
                    countries.find { it.name == option }?.let { country ->
                        localCountry = country
                    }
                },
            )
        }
    }
}