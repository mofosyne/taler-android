/*
 * This file is part of GNU Taler
 * (C) 2020 Taler Systems S.A.
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

package net.taler.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.math.max

@Serializable
data class Timestamp(
    @SerialName("t_s")
    @Serializable(NeverSerializer::class)
    private val s: Long,
) : Comparable<Timestamp> {

    companion object {
        private const val NEVER: Long = -1
        fun now(): Timestamp = fromMillis(System.currentTimeMillis())
        fun never(): Timestamp = Timestamp(NEVER)
        fun fromMillis(ms: Long) = Timestamp(ms / 1000L)
    }

    val ms: Long = s * 1000L

    operator fun minus(other: Timestamp): RelativeTime = when {
        ms == NEVER -> RelativeTime.fromMillis(RelativeTime.FOREVER)
        other.ms == NEVER -> throw Error("Invalid argument for timestamp comparison")
        ms < other.ms -> RelativeTime.fromMillis(0)
        else -> RelativeTime.fromMillis(ms - other.ms)
    }

    operator fun minus(other: RelativeTime): Timestamp = when {
        ms == NEVER -> this
        other.ms == RelativeTime.FOREVER -> fromMillis(0)
        else -> fromMillis(max(0, ms - other.ms))
    }

    override fun compareTo(other: Timestamp): Int {
        return if (ms == NEVER) {
            if (other.ms == NEVER) 0
            else 1
        } else {
            if (other.ms == NEVER) -1
            else ms.compareTo(other.ms)
        }
    }

}

@Serializable
data class RelativeTime(
    /**
     * Duration in microseconds or "forever" to represent an infinite duration.
     * Numeric values are capped at 2^53 - 1 inclusive.
     */
    @SerialName("d_us")
    @Serializable(ForeverSerializer::class)
    private val s: Long,
) {
    val ms: Long = s * 1000L

    companion object {
        internal const val FOREVER: Long = -1
        fun forever(): RelativeTime = fromMillis(FOREVER)
        fun fromMillis(ms: Long) = RelativeTime(ms / 100L)
    }
}

internal abstract class MinusOneSerializer(private val keyword: String) :
    JsonTransformingSerializer<Long>(Long.serializer()) {

    override fun transformDeserialize(element: JsonElement): JsonElement {
        return if (element.jsonPrimitive.contentOrNull == keyword) return JsonPrimitive(-1)
        else super.transformDeserialize(element)
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        return if (element.jsonPrimitive.longOrNull == -1L) return JsonPrimitive(keyword)
        else element
    }
}

internal object NeverSerializer : MinusOneSerializer("never")
internal object ForeverSerializer : MinusOneSerializer("forever")
