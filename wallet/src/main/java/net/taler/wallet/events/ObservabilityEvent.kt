/*
 * This file is part of GNU Taler
 * (C) 2024 Taler Systems S.A.
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

package net.taler.wallet.events

import androidx.annotation.StringRes
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import net.taler.common.Timestamp
import net.taler.wallet.R
import net.taler.wallet.backend.TalerErrorInfo

@Serializable
sealed class ObservabilityEvent {
    @get:StringRes
    abstract val titleRes: Int

    @Serializable
    @SerialName("http-fetch-start")
    data class HttpFetchStart(
        val id: String,
        @SerialName("when")
        val timestamp: Timestamp,
        val url: String,

        override val titleRes: Int = R.string.event_http_fetch_start,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("http-fetch-finish-success")
    data class HttpFetchFinishSuccess(
        val id: String,
        @SerialName("when")
        val timestamp: Timestamp,
        val url: String,
        val status: Int,

        override val titleRes: Int = R.string.event_http_fetch_finish_success,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("http-fetch-finish-error")
    data class HttpFetchFinishError(
        val id: String,
        @SerialName("when")
        val timestamp: Timestamp,
        val url: String,
        val error: TalerErrorInfo,

        override val titleRes: Int = R.string.event_http_fetch_finish_error,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("db-query-start")
    data class DbQueryStart(
        val name: String,
        val location: String,

        override val titleRes: Int = R.string.event_db_query_start,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("db-query-finish-success")
    data class DbQueryFinishSuccess(
        val name: String,
        val location: String,

        override val titleRes: Int = R.string.event_db_query_finish_success,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("db-query-finish-error")
    data class DbQueryFinishError(
        val name: String,
        val location: String,

        override val titleRes: Int = R.string.event_db_query_finish_error,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("request-start")
    data class RequestStart(
        override val titleRes: Int = R.string.event_request_start,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("request-finish-success")
    data class RequestFinishSuccess(
        override val titleRes: Int = R.string.event_request_finish_success,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("request-finish-error")
    data class RequestFinishError(
        override val titleRes: Int = R.string.event_request_finish_error,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("task-start")
    data class TaskStart(
        val taskId: String,

        override val titleRes: Int = R.string.event_task_start,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("task-stop")
    data class TaskStop(
        val taskId: String,

        override val titleRes: Int = R.string.event_task_stop,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("task-reset")
    data class TaskReset(
        val taskId: String,

        override val titleRes: Int = R.string.event_task_reset,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("declare-task-dependency")
    data class DeclareTaskDependency(
        val taskId: String,

        override val titleRes: Int = R.string.event_declare_task_dependency,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("crypto-start")
    data class CryptoStart(
        val operation: String,

        override val titleRes: Int = R.string.event_crypto_start,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("crypto-finish-success")
    data class CryptoFinishSuccess(
        val operation: String,

        override val titleRes: Int = R.string.event_crypto_finished_success,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("crypto-finish-error")
    data class CryptoFinishError(
        val operation: String,

        override val titleRes: Int = R.string.event_crypto_finished_error,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("sheperd-task-result")
    data class ShepherdTaskResult(
        val resultType: String,

        override val titleRes: Int = R.string.event_shepherd_task_result,
    ): ObservabilityEvent()

    @Serializable
    @SerialName("unknown")
    data class Unknown(
        override val titleRes: Int = R.string.event_unknown,
    ): ObservabilityEvent()
}