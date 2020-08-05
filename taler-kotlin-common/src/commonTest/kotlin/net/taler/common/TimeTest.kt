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

import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json.Default.parse
import kotlinx.serialization.json.Json.Default.stringify
import net.taler.common.Timestamp.Companion.NEVER
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

// TODO test other functionality of Timestamp and Duration
@UnstableDefault
class TimeTest {

    @Test
    fun testSerialize() {
        for (i in 0 until 42) {
            val t = Random.nextLong()
            assertEquals("""{"t_ms":$t}""", stringify(Timestamp.serializer(), Timestamp(t)))
        }
        assertEquals("""{"t_ms":"never"}""", stringify(Timestamp.serializer(), Timestamp(NEVER)))
    }

    @Test
    fun testDeserialize() {
        for (i in 0 until 42) {
            val t = Random.nextLong()
            assertEquals(Timestamp(t), parse(Timestamp.serializer(), """{ "t_ms": $t }"""))
        }
        assertEquals(Timestamp(NEVER), parse(Timestamp.serializer(), """{ "t_ms": "never" }"""))
    }

}
