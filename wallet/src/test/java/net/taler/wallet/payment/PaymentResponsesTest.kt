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

package net.taler.wallet.payment

import kotlinx.serialization.json.Json
import org.junit.Test

class PaymentResponsesTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun testInsufficientBalanceResponse() {
        val jsonStr = """
        {
          "status": "insufficient-balance",
          "contractTerms": {
            "summary": "Gummy bears (BFH)",
            "amount": "CHF:0.3",
            "fulfillment_message": "\/Enjoy+your+",
            "auto_refund": {
              "d_ms": 300000
            },
            "products": [],
            "h_wire": "TAHX3QPREEV64GN5SJRNRJD1EF0ZK50X8Y4BZAGEJSFQ7YVYAW1V3DVTFWVG2RXETPX05ZB9CQSHHXGFX10KRS76JK0XHC60F0YS268",
            "wire_method": "x-taler-bank",
            "order_id": "2020.240-01MD5F476HMXW",
            "timestamp": {
              "t_s": 1598538535
            },
            "refund_deadline": {
              "t_s": 1598538835
            },
            "pay_deadline": {
              "t_s": 1598538835
            },
            "wire_transfer_deadline": {
              "t_s": 1598542135
            },
            "max_wire_fee": "CHF:0.1",
            "max_fee": "CHF:0.1",
            "wire_fee_amortization": 10,
            "merchant": {
              "name": "BFH Department Technik und Informatik",
              "instance": "department"
            },
            "exchanges": [],
            "auditors": [],
            "merchant_pub": "ZMVDPGGAESGYNMZTE4VHDE5QA5BMT7C9A6GR688KGBPMPATF4MKG",
            "nonce": "W4WNY6D82H3Y8AV57FBTW4M9YR633N1ARRMBJ6R22MWPYB51JS00"
          },
          "proposalId": "BYWTGTHW2TM1FJSM923KD5ZGGFACRYB8EFA461R8AHVK7T9S9ZZG",
          "amountRaw": "CHF:0.3"
        }
    """.trimIndent()
        val response = json.decodeFromString(PreparePayResponse.serializer(), jsonStr)
        println(response)
    }

}
