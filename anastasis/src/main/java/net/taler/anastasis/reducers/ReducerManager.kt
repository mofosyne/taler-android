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

package net.taler.anastasis.reducers

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.taler.anastasis.Utils
import net.taler.anastasis.backend.AnastasisReducerApi
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.models.CountryInfo
import net.taler.anastasis.models.ReducerArgs
import net.taler.anastasis.models.ReducerState
import org.json.JSONObject
import kotlin.time.Duration.Companion.seconds

class ReducerManager(
    private val state: MutableStateFlow<ReducerState?>,
    private val api: AnastasisReducerApi,
    private val scope: CoroutineScope,
) {
    private companion object {
        const val PROVIDER_SYNC_PERIOD = 20
    }

    private var providerSyncingJob: Job? = null

    // TODO: error handling!

    fun startBackup() = scope.launch {
        state.value = api.startBackup()
    }

    fun startRecovery() = scope.launch {
        state.value = api.startRecovery()
    }

    fun back() = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "back")
                .onSuccess { newState ->
                    state.value = newState
                }
        }
    }

    fun selectContinent(continent: ContinentInfo) = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "select_continent") {
                put("continent", continent.name)
            }.onSuccess { newState ->
                state.value = newState
            }
        }
    }

    fun selectCountry(country: CountryInfo) = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "select_country") {
                put("country_code", country.code)
                // TODO: stop hardcoding currency!
                put("currency", "EUR")
            }.onSuccess { newState ->
                state.value = newState
            }
        }
    }

    fun enterUserAttributes(userAttributes: Map<String, String>) = scope.launch {
        state.value?.let {  initialState ->
            api.reduceAction(initialState, "enter_user_attributes") {
                put("identity_attributes", JSONObject(userAttributes))
            }.onSuccess { newState ->
                state.value = newState
            }
        }
    }

    fun startSyncingProviders() {
        if (providerSyncingJob != null) return
        providerSyncingJob = Utils.tickerFlow(PROVIDER_SYNC_PERIOD.seconds)
            .onEach {
                state.value?.let { initialState ->
                    // Only run sync when not all providers are synced
                    if (initialState is ReducerState.Backup) {
                        initialState.authenticationProviders?.flatMap {
                            listOf(it.value)
                        }?.fold(false) { a, b ->
                            a || (b !is AuthenticationProviderStatus.Ok)
                        }?.let { sync ->
                            if (!sync) {
                                Log.d("ReducerManager", "All providers are synced")
                                return@onEach
                            }
                        }
                    }
                    Log.d("ReducerManager", "Syncing providers...")
                    api.reduceAction(initialState, "sync_providers")
                        .onSuccess { newState ->
                            state.value = newState
                        }
                        .onError {
                            Log.d("ReducerManager", "Sync error: $it")
                        }
                }
            }
            .catch {
                Log.d("ReducerManager", "Could not sync providers")
            }
            .launchIn(scope)
    }

    fun stopSyncingProviders() {
        providerSyncingJob?.cancel()
        providerSyncingJob = null
    }

    fun addAuthentication(args: ReducerArgs.AddAuthentication) = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "add_authentication", args)
                .onSuccess { newState ->
                    state.value = newState
                }
        }
    }

    fun deleteAuthentication(index: Int) = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "delete_authentication") {
                put("index", index)
            }.onSuccess { newState ->
                state.value = newState
            }
        }
    }
}