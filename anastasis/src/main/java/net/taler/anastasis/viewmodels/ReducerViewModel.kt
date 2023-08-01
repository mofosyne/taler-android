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

package net.taler.anastasis.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.taler.anastasis.Routes
import net.taler.anastasis.backend.AnastasisReducerApi
import net.taler.anastasis.backend.TalerErrorInfo
import net.taler.anastasis.models.BackupStates
import net.taler.anastasis.models.RecoveryStates
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.reducers.ReducerManager
import javax.inject.Inject

interface ReducerViewModelI {
    val reducerManager: ReducerManager?
    val reducerState: StateFlow<ReducerState?>
    val reducerError: StateFlow<TalerErrorInfo?>

    fun goBack(): Boolean
    fun goHome()
    fun cleanError()
}

@HiltViewModel
class ReducerViewModel @Inject constructor(): ViewModel(), ReducerViewModelI {
    private val api = AnastasisReducerApi()
    override val reducerManager: ReducerManager?

    private val _reducerState = MutableStateFlow<ReducerState?>(null)
    override val reducerState = _reducerState.asStateFlow()
    private val _reducerError = MutableStateFlow<TalerErrorInfo?>(null)
    override val reducerError = _reducerError.asStateFlow()
    private val _navRoute = MutableStateFlow(Routes.Home.route)
    val navRoute = _navRoute.asStateFlow()

    init {
        reducerManager = ReducerManager(_reducerState, _reducerError, api, viewModelScope)
        viewModelScope.launch {
            _reducerState.collect {
                reducerManager.stopSyncingProviders()
                _navRoute.value = when (it) {
                    is ReducerState.Backup -> when (it.backupState) {
                        BackupStates.ContinentSelecting -> Routes.SelectContinent.route
                        BackupStates.CountrySelecting -> Routes.SelectCountry.route
                        BackupStates.UserAttributesCollecting -> Routes.SelectUserAttributes.route
                        BackupStates.AuthenticationsEditing -> {
                            reducerManager.startSyncingProviders()
                            Routes.SelectAuthMethods.route
                        }
                        BackupStates.PoliciesReviewing -> Routes.ReviewPolicies.route
                        BackupStates.SecretEditing -> Routes.EditSecret.route
                        BackupStates.TruthsPaying -> TODO()
                        BackupStates.PoliciesPaying -> TODO()
                        BackupStates.BackupFinished -> Routes.BackupFinished.route
                    }
                    is ReducerState.Recovery -> when (it.recoveryState) {
                        RecoveryStates.ContinentSelecting -> Routes.SelectContinent.route
                        RecoveryStates.CountrySelecting -> Routes.SelectCountry.route
                        RecoveryStates.UserAttributesCollecting -> Routes.SelectUserAttributes.route
                        RecoveryStates.SecretSelecting -> TODO()
                        RecoveryStates.ChallengeSelecting -> TODO()
                        RecoveryStates.ChallengePaying -> TODO()
                        RecoveryStates.ChallengeSolving -> TODO()
                        RecoveryStates.RecoveryFinished -> TODO()
                    }
                    else -> Routes.Home.route
                }
            }
        }
    }

    override fun goBack(): Boolean = when (val state = reducerState.value) {
        is ReducerState.Backup -> when (state.backupState) {
            BackupStates.ContinentSelecting -> {
                goHome()
                false
            }
            else -> {
                reducerManager?.back()
                false
            }
        }
        is ReducerState.Recovery -> when(state.recoveryState) {
            RecoveryStates.ContinentSelecting -> {
                goHome()
                false
            }
            else -> {
                reducerManager?.back()
                false
            }
        }
        is ReducerState.Error -> {
            reducerManager?.back()
            false
        }
        else -> true
    }

    override fun goHome() {
        _reducerState.value = null
    }

    override fun cleanError() {
        _reducerError.value = null
    }
}

class FakeReducerViewModel(
    state: ReducerState,
    error: TalerErrorInfo? = null,
): ReducerViewModelI {
    override val reducerManager = null
    private val _reducerState = MutableStateFlow<ReducerState?>(state)
    override val reducerState: StateFlow<ReducerState?> = _reducerState.asStateFlow()
    private val _reducerError = MutableStateFlow(error)
    override val reducerError: StateFlow<TalerErrorInfo?> = _reducerError.asStateFlow()

    override fun goBack(): Boolean = false

    override fun goHome() {
        _reducerState.value = null
    }

    override fun cleanError() {
        _reducerError.value = null
    }
}