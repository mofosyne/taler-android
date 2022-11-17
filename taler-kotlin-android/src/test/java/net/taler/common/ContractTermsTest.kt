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

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ContractTermsTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun test() {
        val jsonStr = """
            {
              "amount":"TESTKUDOS:0.5",
              "extra":{
                "article_name":"1._The_Free_Software_Definition"
              },
              "fulfillment_url":"https://shop.test.taler.net/essay/1._The_Free_Software_Definition",
              "summary":"Essay: 1. The Free Software Definition",
              "refund_deadline":{"t_s":"never"},
              "wire_transfer_deadline":{"t_s":1596128564},
              "products":[],
              "h_wire":"KV40K023N8EC1F5100TYNS23C4XN68Y1Z3PTJSWFGTMCNYD54KT4S791V2VQ91SZANN86VDAA369M4VEZ0KR6DN71EVRRZA71K681M0",
              "wire_method":"x-taler-bank",
              "order_id":"2020.212-01M9VKEAPF76C",
              "timestamp":{"t_s":1596128114},
              "pay_deadline":{"t_s":"never"},
              "max_wire_fee":"TESTKUDOS:1",
              "max_fee":"TESTKUDOS:1",
              "wire_fee_amortization":3,
              "merchant_base_url":"https://backend.test.taler.net/instances/blog/",
              "merchant":{"name":"Blog","instance":"blog"},
              "exchanges":[
                {
                    "url":"https://exchange.test.taler.net/",
                    "master_pub":"DY95EXAHQ2BKM2WK9YHZHYG1R7PPMMJPY14FNGP662DAKE35AKQG"
                },
                {
                    "url":"https://exchange.test.taler.net/",
                    "master_pub":"DY95EXAHQ2BKM2WK9YHZHYG1R7PPMMJPY14FNGP662DAKE35AKQG"}
                ],
                "auditors":[],
                "merchant_pub":"8DR9NKSZY1CXFRE47NEYXM0K85C4ZGAYH7Y7VZ22GPNF0BRFNYNG",
                "nonce":"FK8ZKJRV6VX6YFAG4CDSC6W0DWD084Q09DP81ANF30GRFQYM2KPG"
              }
        """.trimIndent()
        val contractTerms: ContractTerms = json.decodeFromString(jsonStr)
        assertEquals("Essay: 1. The Free Software Definition", contractTerms.summary)
        assertEquals(Timestamp.never(), contractTerms.refundDeadline)
    }

}
