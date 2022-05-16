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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class VersionTest {

    @Test
    fun testParse() {
        assertNull(Version.parse(""))
        assertNull(Version.parse("foo"))
        assertNull(Version.parse("foo:bar:foo"))
        assertNull(Version.parse("0:0:0:"))
        assertNull(Version.parse("0:0:"))
        assertEquals(Version(0, 0, 0), Version.parse("0:0:0"))
        assertEquals(Version(1, 2, 3), Version.parse("1:2:3"))
        assertEquals(Version(1337, 42, 23), Version.parse("1337:42:23"))
    }

    @Test
    fun testComparision() {
        assertEquals(
            Version.VersionMatchResult(true, 0),
            Version.parse("0:0:0")!!.compare(Version.parse("0:0:0"))
        )
        assertEquals(
            Version.VersionMatchResult(true, -1),
            Version.parse("0:0:0")!!.compare(Version.parse("1:0:1"))
        )
        assertEquals(
            Version.VersionMatchResult(true, -1),
            Version.parse("0:0:0")!!.compare(Version.parse("1:5:1"))
        )
        assertEquals(
            Version.VersionMatchResult(false, -1),
            Version.parse("0:0:0")!!.compare(Version.parse("1:5:0"))
        )
        assertEquals(
            Version.VersionMatchResult(false, 1),
            Version.parse("1:0:0")!!.compare(Version.parse("0:5:0"))
        )
        assertEquals(
            Version.VersionMatchResult(true, 0),
            Version.parse("1:0:1")!!.compare(Version.parse("1:5:1"))
        )
    }

}
