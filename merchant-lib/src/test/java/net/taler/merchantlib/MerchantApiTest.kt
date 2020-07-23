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

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import net.taler.common.Amount
import net.taler.common.ContractProduct
import net.taler.common.ContractTerms
import net.taler.merchantlib.MockHttpClient.giveJsonResponse
import net.taler.merchantlib.MockHttpClient.httpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantApiTest {

    private val api = MerchantApi(httpClient)
    private val merchantConfig = MerchantConfig(
        baseUrl = "http://example.net/",
        instance = "testInstance",
        apiKey = "apiKeyFooBar"
    )

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
                            "price": "${product.price.toJSONString()}",
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
        api.postOrder(merchantConfig, contractTerms).assertSuccess {
            assertEquals(PostOrderResponse("test"), it)
        }

        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders",
            statusCode = HttpStatusCode.NotFound
        ) {
            """{
                "code": 2000,
                "hint": "merchant instance unknown"
            }"""
        }
        api.postOrder(merchantConfig, contractTerms).assertFailure {
            assertTrue(it.contains("2000"))
            assertTrue(it.contains("merchant instance unknown"))
        }
    }

    @Test
    fun testCheckOrder() = runBlocking {
        val orderId = "orderIdFoo"
        val unpaidResponse = CheckPaymentResponse.Unpaid(false, "http://taler.net/foo")
        httpClient.giveJsonResponse("http://example.net/instances/testInstance/private/orders/$orderId") {
            """{
                "paid": ${unpaidResponse.paid},
                "taler_pay_uri": "${unpaidResponse.talerPayUri}"
            }""".trimIndent()
        }
        api.checkOrder(merchantConfig, orderId).assertSuccess {
            assertEquals(unpaidResponse, it)
        }

        httpClient.giveJsonResponse(
            "http://example.net/instances/testInstance/private/orders/$orderId",
            statusCode = HttpStatusCode.NotFound
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

}
