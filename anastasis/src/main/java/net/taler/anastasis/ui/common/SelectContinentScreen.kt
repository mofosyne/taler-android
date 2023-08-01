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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import net.taler.anastasis.R
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.ui.reusable.components.Picker
import net.taler.anastasis.ui.reusable.pages.WizardPage
import net.taler.anastasis.ui.theme.LocalSpacing
import net.taler.anastasis.viewmodels.FakeReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModel
import net.taler.anastasis.viewmodels.ReducerViewModelI

@Composable
fun SelectContinentScreen(
    viewModel: ReducerViewModelI = hiltViewModel<ReducerViewModel>(),
) {
    val reducerState by viewModel.reducerState.collectAsState()
    val continents = when (val state = reducerState) {
        is ReducerState.Backup -> state.continents
        is ReducerState.Recovery -> state.continents
        else -> error("invalid reducer state type")
    } ?: emptyList()

    val selectedContinent = when (val state = reducerState) {
        is ReducerState.Backup -> state.selectedContinent
        is ReducerState.Recovery -> state.selectedContinent
        else -> error("invalid reducer state type")
    }

    var localContinent by remember {
        mutableStateOf(selectedContinent?.let { selected ->
            continents.find { it.name == selected }
        })
    }

    WizardPage(
        title = stringResource(R.string.select_continent_title),
        showPrev = false,
        enableNext = localContinent != null,
        onBackClicked = { viewModel.goHome() },
        onNextClicked = {
            localContinent?.let {
                viewModel.reducerManager?.selectContinent(it)
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
                label = stringResource(R.string.continent),
                initialOption = localContinent?.name,
                options = continents.map { it.name }.toSet(),
                onOptionChanged = { option ->
                    continents.find { it.name == option }?.let { continent ->
                        localContinent = continent
                    }
                },
            )
        }
    }
}

@Preview
@Composable
fun SelectContinentScreenPreview() {
    SelectContinentScreen(
        viewModel = FakeReducerViewModel(
            state = ReducerState.Backup(
                backupState = BackupStates.ContinentSelecting,
                selectedContinent = "Europe",
                continents = listOf(
                    ContinentInfo(name = "Europe"),
                    ContinentInfo(name = "North America"),
                )
            )
        )
    )
}