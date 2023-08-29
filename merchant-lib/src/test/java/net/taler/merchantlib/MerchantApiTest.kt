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

import io.ktor.http.HttpStatusCode.Companion.NotFound
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.taler.common.Amount
import net.taler.common.ContractProduct
import net.taler.common.ContractTerms
import net.taler.common.Timestamp
import net.taler.merchantlib.MockHttpClient.giveJsonResponse
import net.taler.merchantlib.MockHttpClient.httpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@ExperimentalCoroutinesApi
class MerchantApiTest {

    private val api = MerchantApi(httpClient, UnconfinedTestDispatcher())
    private val merchantConfig = MerchantConfig(
        baseUrl = "http://example.net/instances/testInstance",
        apiKey = "apiKeyFooBar"
    )
    private val orderId = "orderIdFoo"

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
        api.getConfig("https://backend.int.taler.net").assertSuccess {
            assertEquals(ConfigResponse("0:0:0", "INTKUDOS"), it)
        }
    }

    @Test
    fun testPostOrder() = runBlocking {
        val product = ContractProduct(
            productId = "foo",
            description = "bar",
            price = Amount("TEST", 1, 0),
            quantity = 2
        )
        val contractTerms = ContractTerms(
            summary = "test",
            amount = Amount("TEST", 2, 1),
            fulfillmentUrl = "http://example.org",
            products = listOf(product)
        )
        val request = PostOrderRequest(contractTerms)
        val contractTermsJson = """
            {
                "order": {
                    "summary": "${contractTerms.summary}",
                    "amount": "${contractTerms.amount.toJSONString()}",
                    "fulfillment_url": "${contractTerms.fulfillmentUrl}",
                    "products": [
                        {
                            "product_id": "${product.productId}",
                            "description": "${product.description}",
                            "price": "${product.price!!.toJSONString()}",
                            "quantity": ${product.quantity}
                        }
                    ]
                }
            }
        """.trimIndent()
        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders",
            contractTermsJson
        ) {
            """{"order_id": "test"}"""
        }
        api.postOrder(merchantConfig, request).assertSuccess {
            assertEquals(PostOrderResponse("test"), it)
        }

        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders",
            statusCode = NotFound
        ) {
            """{
                "code": 2000,
                "hint": "merchant instance unknown"
            }"""
        }
        api.postOrder(merchantConfig, request).assertFailure {
            assertTrue(it.contains("2000"))
            assertTrue(it.contains("merchant instance unknown"))
        }
    }

    @Test
    fun testCheckOrder() = runBlocking {
        val unpaidResponse = CheckPaymentResponse.Unpaid(false, "http://taler.net/foo")
        httpClient.giveJsonResponse("http://example.net/instances/testInstance/private/orders/$orderId") {
            """{
                "order_status": "unpaid",
                "paid": ${unpaidResponse.paid},
                "taler_pay_uri": "${unpaidResponse.talerPayUri}"
            }""".trimIndent()
        }
        api.checkOrder(merchantConfig, orderId).assertSuccess {
            assertEquals(unpaidResponse, it)
        }

        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders/$orderId",
            statusCode = NotFound
        ) {
            """{
                "code": 2909,
                "hint": "Did not find contract terms for order in DB"
            }"""
        }
        api.checkOrder(merchantConfig, orderId).assertFailure {
            assertTrue(it.contains("2909"))
            assertTrue(it.contains("Did not find contract terms for order in DB"))
        }
    }

    @Test
    fun testDeleteOrder() = runBlocking {
        httpClient.giveJsonResponse("http://example.net/instances/testInstance/private/orders/$orderId") {
            "{}"
        }
        api.deleteOrder(merchantConfig, orderId).assertSuccess {}

        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders/$orderId",
            statusCode = NotFound
        ) {
            """{
                "code": 2511,
                "hint": "Order unknown"
                }
            """.trimIndent()
        }
        api.deleteOrder(merchantConfig, orderId).assertFailure {
            assertTrue(it.contains("2511"))
            assertTrue(it.contains("Order unknown"))
        }
    }

    @Test
    fun testGetOrderHistory() = runBlocking {
        httpClient.giveJsonResponse("http://example.net/instances/testInstance/private/orders") {
            """{  "orders": [
                    {
                      "order_id": "2020.217-0281FGXCS25P2",
                      "row_id": 183,
                      "timestamp": {
                        "t_s": 1596542338
                      },
                      "amount": "TESTKUDOS:1",
                      "summary": "Chips",
                      "refundable": true,
                      "paid": true
                    },
                    {
                      "order_id": "2020.216-01G2ZPXSP6BYT",
                      "row_id": 154,
                      "timestamp": {
                        "t_s": 1596468174
                      },
                      "amount": "TESTKUDOS:0.8",
                      "summary": "Peanuts",
                      "refundable": false,
                      "paid": false
                    }
                ]
            }""".trimIndent()
        }
        api.getOrderHistory(merchantConfig).assertSuccess {
            assertEquals(2, it.orders.size)

            val order1 = it.orders[0]
            assertEquals(Amount("TESTKUDOS", 1, 0), order1.amount)
            assertEquals("2020.217-0281FGXCS25P2", order1.orderId)
            assertEquals(true, order1.paid)
            assertEquals(true, order1.refundable)
            assertEquals("Chips", order1.summary)
            assertEquals(Timestamp.fromMillis(1596542338000), order1.timestamp)

            val order2 = it.orders[1]
            assertEquals(Amount("TESTKUDOS", 0, 80000000), order2.amount)
            assertEquals("2020.216-01G2ZPXSP6BYT", order2.orderId)
            assertEquals(false, order2.paid)
            assertEquals(false, order2.refundable)
            assertEquals("Peanuts", order2.summary)
            assertEquals(Timestamp.fromMillis(1596468174000), order2.timestamp)
        }
    }

    @Test
    fun testGiveRefund() = runBlocking {
        httpClient.giveJsonResponse("http://example.net/instances/testInstance/private/orders/$orderId/refund") {
            """{
                "taler_refund_uri": "taler://refund/foo/bar"
            }""".trimIndent()
        }
        val request = RefundRequest(
            refund = Amount("TESTKUDOS", 5, 0),
            reason = "Give me my money back now!!!"
        )
        api.giveRefund(merchantConfig, orderId, request).assertSuccess {
            assertEquals("taler://refund/foo/bar", it.talerRefundUri)
        }
    }
}
