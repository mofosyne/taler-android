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
import net.taler.anastasis.backend.AnastasisReducerApi
import net.taler.anastasis.backend.TalerErrorInfo
import net.taler.anastasis.backend.Tasks
import net.taler.anastasis.models.AggregatedPolicyMetaInfo
import net.taler.anastasis.models.AuthMethod
import net.taler.anastasis.models.AuthenticationProviderStatus
import net.taler.anastasis.models.ContinentInfo
import net.taler.anastasis.models.CoreSecret
import net.taler.anastasis.models.CountryInfo
import net.taler.anastasis.models.Policy
import net.taler.anastasis.models.ReducerState
import net.taler.anastasis.shared.Utils
import net.taler.common.Timestamp
import kotlin.time.Duration.Companion.seconds
import net.taler.anastasis.models.ReducerArgs.*

class ReducerManager(
    private val state: MutableStateFlow<ReducerState?>,
    private val error: MutableStateFlow<TalerErrorInfo?>,
    private val tasks: MutableStateFlow<Tasks>,
    private val api: AnastasisReducerApi,
    private val scope: CoroutineScope,
) {

    private companion object {
        const val PROVIDER_SYNC_PERIOD = 20
        const val POLICY_DISCOVERY_PERIOD = 20
    }

    private var providerSyncingJob: Job? = null
    private var policyDiscoveryJob: Job? = null

    private fun addTask(type: Tasks.Type = Tasks.Type.Foreground) {
        tasks.value = tasks.value.addTask(type)
    }

    private fun onSuccess(newState: ReducerState, taskType: Tasks.Type = Tasks.Type.Foreground) {
        tasks.value = tasks.value.removeTask(taskType)
        state.value = newState
    }

    private fun onError(info: TalerErrorInfo, taskType: Tasks.Type = Tasks.Type.Foreground) {
        tasks.value = tasks.value.removeTask(taskType)
        error.value = info
    }

    fun startBackup() = scope.launch {
        state.value = api.startBackup()
    }

    fun startRecovery() = scope.launch {
        state.value = api.startRecovery()
    }

    fun back() = scope.launch {
        state.value?.let { initialState ->
            api.reduceAction(initialState, "back")
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun next() = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "next")
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun selectContinent(continent: ContinentInfo) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "select_continent", SelectContinent(
                continent = continent.name,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun selectCountry(country: CountryInfo) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "select_country", SelectCountry(
                countryCode = country.code,
                // TODO: stop hardcoding currency!
                currency = "EUR",
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun enterUserAttributes(userAttributes: Map<String, String>) = scope.launch {
        state.value?.let {  initialState ->
            addTask()
            api.reduceAction(initialState, "enter_user_attributes", EnterUserAttributes(
                identityAttributes = userAttributes,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun addProvider(url: String) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "add_provider", AddProvider(
                providerUrl = url,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun deleteProvider(url: String) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "delete_provider", DeleteProvider(
                providerUrl = url,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun startSyncingProviders() {
        if (providerSyncingJob != null) return
        providerSyncingJob = Utils.tickerFlow(PROVIDER_SYNC_PERIOD.seconds)
            .onEach {
                state.value?.let { initialState ->
                    when (initialState) {
                        is ReducerState.Backup -> initialState.authenticationProviders
                        is ReducerState.Recovery -> initialState.authenticationProviders
                        else -> error("invalid reducer type")
                    }?.flatMap {
                        listOf(it.value)
                    }?.fold(false) { a, b ->
                        a || (b is AuthenticationProviderStatus.NotContacted)
                    }?.let { sync ->
                        if (!sync) {
                            Log.d("ReducerManager", "All providers are synced")
                            stopSyncingProviders()
                            return@onEach
                        }
                    }
                    Log.d("ReducerManager", "Syncing providers...")
                    addTask(Tasks.Type.Background)
                    api.reduceAction(initialState, "sync_providers")
                        .onSuccess { this@ReducerManager.onSuccess(it, Tasks.Type.Background) }
                        .onError { this@ReducerManager.onError(it, Tasks.Type.Background) }
                }
            }
            .catch {
                Log.d("ReducerManager", "Could not sync providers: ${it.stackTraceToString()}")
            }
            .launchIn(scope)
    }

    fun stopSyncingProviders() {
        providerSyncingJob?.cancel()
        providerSyncingJob = null
    }

    fun addAuthentication(method: AuthMethod) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "add_authentication", AddAuthentication(
                authenticationMethod = method,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun deleteAuthentication(index: Int) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "delete_authentication", DeleteAuthentication(
                authenticationMethod = index,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun addPolicy(policy: Policy) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "add_policy", AddPolicy(
                policy = policy.methods,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun updatePolicy(index: Int, policy: Policy) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "update_policy", UpdatePolicy(
                policyIndex = index,
                policy = policy.methods,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun deletePolicy(index: Int) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "delete_policy", DeletePolicy(
                policyIndex = index,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun enterSecretName(secretName: String) = scope.launch {
        state.value?.let { initialState ->
            addTask(Tasks.Type.None)
            api.reduceAction(initialState, "enter_secret_name", EnterSecretName(
                name = secretName,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun enterSecret(
        secret: CoreSecret,
        expiration: Timestamp,
    ) = scope.launch {
        state.value?.let { initialState ->
            addTask(Tasks.Type.None)
            api.reduceAction(initialState, "enter_secret", EnterSecret(
                secret = secret,
                expiration = expiration,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun updateSecretExpiration(expiration: Timestamp) = scope.launch {
        state.value?.let { initialState ->
            addTask(Tasks.Type.None)
            api.reduceAction(initialState, "update_expiration", UpdateExpiration(
                expiration = expiration,
            ))
                .onSuccess { onSuccess(it) }
                .onError { onError(it) }
        }
    }

    fun startDiscoveringPolicies() {
        if (policyDiscoveryJob != null) return
        policyDiscoveryJob = Utils.tickerFlow(POLICY_DISCOVERY_PERIOD.seconds)
            .onEach {
                state.value?.let { initialState ->
                    Log.d("ReducerManager", "Discovering policies...")
                    val discoveryState = (initialState as? ReducerState.Recovery)?.discoveryState
                    // Stop discovering when discovery is finished
                    if (discoveryState != null && discoveryState.state == "finished") {
                        Log.d("ReducerManager", "All secrets were discovered")
                        this@ReducerManager.stopDiscoveringPolicies()
                    } else {
                        addTask(Tasks.Type.Background)
                        api.discoverPolicies(initialState)
                            .onSuccess { this@ReducerManager.onSuccess(it, Tasks.Type.Background) }
                            .onError { this@ReducerManager.onError(it, Tasks.Type.Background) }
                    }
                }
            }
            .catch {
                Log.d("ReducerManager", "Could not discover policies: ${it.stackTraceToString()}")
            }
            .launchIn(scope)
    }

    fun stopDiscoveringPolicies() {
        policyDiscoveryJob?.cancel()
        policyDiscoveryJob = null
    }

    fun selectVersion(version: AggregatedPolicyMetaInfo) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "select_version", version)
                .onSuccess { this@ReducerManager.onSuccess(it) }
                .onError { this@ReducerManager.onError(it) }
        }
    }

    fun selectChallenge(uuid: String) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "select_challenge", SelectChallenge(
                uuid = uuid,
            ))
                .onSuccess { this@ReducerManager.onSuccess(it) }
                .onError { this@ReducerManager.onError(it) }
        }
    }

    fun solveAnswerChallenge(answer: String) = scope.launch {
        state.value?.let { initialState ->
            addTask()
            api.reduceAction(initialState, "solve_challenge", SolveChallengeRequest.Answer(
                answer = answer,
            ))
                .onSuccess { this@ReducerManager.onSuccess(it) }
                .onError { this@ReducerManager.onError(it) }
        }
    }
}