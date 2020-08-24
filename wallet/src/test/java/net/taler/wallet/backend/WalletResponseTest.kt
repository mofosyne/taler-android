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

package net.taler.wallet.backend

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.Json
import net.taler.lib.common.Amount
import net.taler.lib.common.AmountMixin
import net.taler.lib.common.Timestamp
import net.taler.lib.common.TimestampMixin
import net.taler.wallet.balances.BalanceResponse
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addMixIn(Amount::class.java, AmountMixin::class.java)
        .addMixIn(Timestamp::class.java, TimestampMixin::class.java)

    @Test
    fun testBalanceResponse() {
        val serializer = WalletResponse.Success.serializer(BalanceResponse.serializer())
        val response = json.decodeFromString(
            serializer, """
            {
              "type": "response",
              "operation": "getBalances",
              "id": 2,
              "result": {
                "balances": [
                  {
                    "available": "TESTKUDOS:15.8",
                    "pendingIncoming": "TESTKUDOS:0",
                    "pendingOutgoing": "TESTKUDOS:0",
                    "hasPendingTransactions": false,
                    "requiresUserInput": false
                  }
                ]
              }
            }
        """.trimIndent()
        )
        assertEquals(1, response.result.balances.size)
    }

    @Test
    fun testWalletErrorInfo() {
        val infoJson = """
            {
                "talerErrorCode":7001,
                "talerErrorHint":"Error: WALLET_UNEXPECTED_EXCEPTION",
                "details":{
                  "httpStatusCode": 401,
                  "requestUrl": "https:\/\/backend.demo.taler.net\/-\/FSF\/orders\/2020.224-02XC8W52BHH3G\/claim",
                  "requestMethod": "POST"
                },
                "message":"unexpected exception: Error: BUG: invariant violation (purchase status)"
            }
        """.trimIndent()
        val info = json.decodeFromString(WalletErrorInfo.serializer(), infoJson)
        val infoJackson: WalletErrorInfo = mapper.readValue(infoJson)
        println(info.userFacingMsg)
        assertEquals(info.userFacingMsg, infoJackson.userFacingMsg)
    }
}
