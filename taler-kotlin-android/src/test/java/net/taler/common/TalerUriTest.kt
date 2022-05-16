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

import net.taler.common.TalerUri.parseWithdrawUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TalerUriTest {

    @Test
    fun testParseWithdrawUri() {
        // correct parsing
        var uri = "taler://withdraw/bank.example.com/12345"
        var expected = TalerUri.WithdrawUriResult("https://bank.example.com/", "12345")
        assertEquals(expected, parseWithdrawUri(uri))

        // correct parsing with insecure http
        uri = "taler+http://withdraw/bank.example.org/foo"
        expected = TalerUri.WithdrawUriResult("http://bank.example.org/", "foo")
        assertEquals(expected, parseWithdrawUri(uri))

        // correct parsing with long path
        uri = "taler://withdraw/bank.example.com/foo/bar/23/42/1337/1234567890"
        expected =
            TalerUri.WithdrawUriResult("https://bank.example.com/foo/bar/23/42/1337", "1234567890")
        assertEquals(expected, parseWithdrawUri(uri))

        // rejects incorrect scheme
        uri = "talerx://withdraw/bank.example.com/12345"
        assertNull(parseWithdrawUri(uri))

        // rejects incorrect authority
        uri = "taler://withdrawx/bank.example.com/12345"
        assertNull(parseWithdrawUri(uri))

        // rejects incorrect authority with insecure http
        uri = "taler+http://withdrawx/bank.example.com/12345"
        assertNull(parseWithdrawUri(uri))

        // rejects empty withdrawalId
        uri = "taler://withdraw/bank.example.com//"
        assertNull(parseWithdrawUri(uri))

        // rejects empty path and withdrawalId
        uri = "taler://withdraw/bank.example.com////"
        assertNull(parseWithdrawUri(uri))
    }

}
