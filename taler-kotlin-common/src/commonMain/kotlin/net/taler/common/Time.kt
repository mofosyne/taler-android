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
import net.taler.common.Duration.Companion.FOREVER
import kotlin.math.max

expect fun nowMillis(): Long

@Serializable
data class Timestamp(
    @SerialName("t_ms")
    val ms: Long
) : Comparable<Timestamp> {

    companion object {
        const val NEVER: Long = -1  // TODO or UINT64_MAX?
        fun now(): Timestamp = Timestamp(nowMillis())
    }

    /**
     * Returns a copy of this [Timestamp] rounded to seconds.
     */
    fun truncateSeconds(): Timestamp {
        if (ms == NEVER) return Timestamp(ms)
        return Timestamp((ms / 1000L) * 1000L)
    }

    operator fun minus(other: Timestamp): Duration = when {
        ms == NEVER -> Duration(FOREVER)
        other.ms == NEVER -> throw Error("Invalid argument for timestamp comparision")
        ms < other.ms -> Duration(0)
        else -> Duration(ms - other.ms)
    }

    operator fun minus(other: Duration): Timestamp = when {
        ms == NEVER -> this
        other.ms == FOREVER -> Timestamp(0)
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
    /**
     * Duration in milliseconds.
     */
    @SerialName("d_ms")
    val ms: Long
) {
    companion object {
        const val FOREVER: Long = -1  // TODO or UINT64_MAX?
    }
}
