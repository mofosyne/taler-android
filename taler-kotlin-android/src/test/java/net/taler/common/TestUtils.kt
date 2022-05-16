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

import kotlin.random.Random
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
fun getRandomString(minLength: Int = 1, maxLength: Int = Random.nextInt(0, 1337)) =
    (minLength..maxLength)
        .map { Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")

inline fun <reified T : Throwable> assertThrows(
    msg: String? = null,
    function: () -> Any
) {
    try {
        function.invoke()
        fail(msg)
    } catch (e: Exception) {
        assertTrue(e is T)
    }
}
