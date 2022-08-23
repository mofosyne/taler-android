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

package net.taler.wallet.tip

import kotlinx.serialization.json.Json
import net.taler.common.Amount
import org.junit.Test

class TipResponsesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun testConfirmTipResult() {
        val jsonStr = """
            {
            "type": "response", 
            "operation": "acceptTip",
             "id": 47,
            "result": {}
             }
        """.trimIndent()
        json.decodeFromString(ConfirmTipResult.serializer(), jsonStr)
    }

    @Test
    fun testTipPossibleSerializer() {
        val jsonStr = """
        {
            "accepted": false,
            "tipAmountRaw": "ARS:2",
            "exchangeBaseUrl": "http://exchange.taler:8081/",
            "merchantBaseUrl": "http://merchant-backend.taler:9966/",
            "expirationTimestamp": {
                "t_s": 1688217455
            },
            "tipAmountEffective": "ARS:1.4",
            "walletTipId": "SZH86ATJC4NZ427JHFVQ9M3S1TCQKVWSSZGSBW8MQ8VTVWD4M4GG"
        }    
        """.trimIndent()
        val response = json.decodeFromString(PrepareTipResponse.serializer(), jsonStr)
        response as PrepareTipResponse.TipPossibleResponse
        assert(response.walletTipId == "SZH86ATJC4NZ427JHFVQ9M3S1TCQKVWSSZGSBW8MQ8VTVWD4M4GG")
        assert(response.tipAmountEffective == Amount(currency = "ARS", fraction = 40000000, value = 1))
    }

    @Test
    fun testTipAcceptedSerializer() {
        val jsonStr = """
        {
            "accepted": true,
            "tipAmountRaw": "ARS:2",
            "exchangeBaseUrl": "http://exchange.taler:8081/",
            "merchantBaseUrl": "http://merchant-backend.taler:9966/",
            "expirationTimestamp": {
                "t_s": 1688217455
            },
            "tipAmountEffective": "ARS:1.4",
            "walletTipId": "SZH86ATJC4NZ427JHFVQ9M3S1TCQKVWSSZGSBW8MQ8VTVWD4M4GG"
        }  
        """.trimIndent()
        val response = json.decodeFromString(PrepareTipResponse.serializer(), jsonStr)
        assert(response is PrepareTipResponse.AlreadyAcceptedResponse)
        assert((response as PrepareTipResponse.AlreadyAcceptedResponse).walletTipId == "SZH86ATJC4NZ427JHFVQ9M3S1TCQKVWSSZGSBW8MQ8VTVWD4M4GG")
    }

}
