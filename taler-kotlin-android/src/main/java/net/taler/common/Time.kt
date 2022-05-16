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
    @SerialName("t_ms")
    @Serializable(NeverSerializer::class)
    val old_ms: Long? = null,
    @SerialName("t_s")
    @Serializable(NeverSerializer::class)
    private val s: Long? = null,
) : Comparable<Timestamp> {

    constructor(ms: Long) : this(ms, null)

    companion object {
        private const val NEVER: Long = -1
        fun now(): Timestamp = Timestamp(System.currentTimeMillis())
        fun never(): Timestamp = Timestamp(NEVER)
    }

    val ms: Long = if (s != null) {
        s * 1000L
    } else if (old_ms !== null) {
        old_ms
    } else  {
        throw Exception("timestamp didn't have t_s or t_ms")
    }


    /**
     * Returns a copy of this [Timestamp] rounded to seconds.
     */
    fun truncateSeconds(): Timestamp {
        if (ms == NEVER) return Timestamp(ms)
        return Timestamp((ms / 1000L) * 1000L)
    }

    operator fun minus(other: Timestamp): Duration = when {
        ms == NEVER -> Duration(Duration.FOREVER)
        other.ms == NEVER -> throw Error("Invalid argument for timestamp comparision")
        ms < other.ms -> Duration(0)
        else -> Duration(ms - other.ms)
    }

    operator fun minus(other: Duration): Timestamp = when {
        ms == NEVER -> this
        other.ms == Duration.FOREVER -> Timestamp(0)
        else -> Timestamp(max(0, ms - other.ms))
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
data class Duration(
    @SerialName("d_ms")
    @Serializable(ForeverSerializer::class) val old_ms: Long? = null,
    @SerialName("d_s")
    @Serializable(ForeverSerializer::class)
    private val s: Long? = null,
) {
    val ms: Long = if (s != null) {
        s * 1000L
    } else if (old_ms !== null) {
        old_ms
    } else  {
        throw Exception("duration didn't have d_s or d_ms")
    }

    constructor(ms: Long) : this(ms, null)

    companion object {
        internal const val FOREVER: Long = -1
        fun forever(): Duration = Duration(FOREVER)
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
