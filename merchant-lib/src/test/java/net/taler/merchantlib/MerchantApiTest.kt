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

package net.taler.merchantlib

import kotlinx.coroutines.runBlocking
import net.taler.merchantlib.MockHttpClient.giveJsonResponse
import net.taler.merchantlib.MockHttpClient.httpClient
import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantApiTest {

    private val api = MerchantApi(httpClient)

    @Test
    fun testGetConfig() = runBlocking {
        httpClient.giveJsonResponse("https://backend.int.taler.net/config") {
            """
            {
              "currency": "INTKUDOS",
              "version": "0:0:0"
            }
            """.trimIndent()
        }
        val response = api.getConfig("https://backend.int.taler.net")
        assertEquals(ConfigResponse("0:0:0", "INTKUDOS"), response)
    }

}
