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

import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

// TODO test other functionality of Timestamp and Duration
class TimeTest {

    @Test
    fun testSerialize() {
        for (i in 0 until 42) {
            val t = Random.nextLong()
            assertEquals("""{"t_s":$t}""", encodeToString(Timestamp.serializer(), Timestamp(t)))
        }
        assertEquals("""{"t_s":"never"}""", encodeToString(Timestamp.serializer(), Timestamp.never()))
    }

    @Test
    fun testDeserialize() {
        for (i in 0 until 42) {
            val t = Random.nextLong()
            assertEquals(Timestamp(t), decodeFromString(Timestamp.serializer(), """{ "t_s": $t }"""))
        }
        assertEquals(Timestamp.never(), decodeFromString(Timestamp.serializer(), """{ "t_s": "never" }"""))
    }

}
