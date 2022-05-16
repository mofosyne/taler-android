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

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.taler.merchantlib.Response.Companion.response
import io.ktor.serialization.kotlinx.json.*

class MerchantApi(
    private val httpClient: HttpClient = getDefaultHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun getConfig(baseUrl: String): Response<ConfigResponse> = withContext(ioDispatcher) {
        response {
            httpClient.get("$baseUrl/config") as ConfigResponse
        }
    }

    suspend fun postOrder(
        merchantConfig: MerchantConfig,
        orderRequest: PostOrderRequest,
    ): Response<PostOrderResponse> = withContext(ioDispatcher) {
        response {
            httpClient.post(merchantConfig.urlFor("private/orders")) {
                header(Authorization, "ApiKey ${merchantConfig.apiKey}")
                contentType(Json)
                setBody(orderRequest)
            }.body()
        }
    }

    suspend fun checkOrder(
        merchantConfig: MerchantConfig,
        orderId: String,
    ): Response<CheckPaymentResponse> = withContext(ioDispatcher) {
        response {
            httpClient.get(merchantConfig.urlFor("private/orders/$orderId")) {
                header(Authorization, "ApiKey ${merchantConfig.apiKey}")
            }.body()
        }
    }

    suspend fun deleteOrder(
        merchantConfig: MerchantConfig,
        orderId: String,
    ): Response<Unit> = withContext(ioDispatcher) {
        response {
            httpClient.delete(merchantConfig.urlFor("private/orders/$orderId")) {
                header(Authorization, "ApiKey ${merchantConfig.apiKey}")
            } as Unit
        }
    }

    suspend fun getOrderHistory(merchantConfig: MerchantConfig): Response<OrderHistory> =
        withContext(ioDispatcher) {
            response {
                httpClient.get(merchantConfig.urlFor("private/orders")) {
                    header(Authorization, "ApiKey ${merchantConfig.apiKey}")
                } as OrderHistory
            }
        }

    suspend fun giveRefund(
        merchantConfig: MerchantConfig,
        orderId: String,
        request: RefundRequest,
    ): Response<RefundResponse> = withContext(ioDispatcher) {
        response {
            httpClient.post(merchantConfig.urlFor("private/orders/$orderId/refund")) {
                header(Authorization, "ApiKey ${merchantConfig.apiKey}")
                contentType(Json)
                setBody(request)
            } as RefundResponse
        }
    }
}

fun getDefaultHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            retryOnConnectionFailure(true)
        }
    }
    install(ContentNegotiation) {
        json()
    }
}
