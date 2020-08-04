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
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.taler.merchantlib.Response.Companion.response

class MerchantApi(private val httpClient: HttpClient) {

    suspend fun getConfig(baseUrl: String): Response<ConfigResponse> = response {
        httpClient.get("$baseUrl/config") as ConfigResponse
    }

    suspend fun postOrder(
        merchantConfig: MerchantConfig,
        orderRequest: PostOrderRequest
    ): Response<PostOrderResponse> = response {
        httpClient.post(merchantConfig.urlFor("private/orders")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
            contentType(Json)
            body = orderRequest
        } as PostOrderResponse
    }

    suspend fun checkOrder(
        merchantConfig: MerchantConfig,
        orderId: String
    ): Response<CheckPaymentResponse> = response {
        httpClient.get(merchantConfig.urlFor("private/orders/$orderId")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
        } as CheckPaymentResponse
    }

    suspend fun deleteOrder(
        merchantConfig: MerchantConfig,
        orderId: String
    ): Response<Unit> = response {
        httpClient.delete(merchantConfig.urlFor("private/orders/$orderId")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
        } as Unit
    }

    suspend fun getOrderHistory(merchantConfig: MerchantConfig): Response<OrderHistory> = response {
        httpClient.get(merchantConfig.urlFor("private/orders")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
        } as OrderHistory
    }

    suspend fun giveRefund(
        merchantConfig: MerchantConfig,
        orderId: String,
        request: RefundRequest
    ): Response<RefundResponse> = response {
        httpClient.post(merchantConfig.urlFor("private/orders/$orderId/refund")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
            contentType(Json)
            body = request
        } as RefundResponse
    }

}

fun getDefaultHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            retryOnConnectionFailure(true)
        }
    }
    install(JsonFeature) {
        serializer = getSerializer()
    }
}

fun getSerializer() = KotlinxSerializer(
    Json(
        JsonConfiguration(
            encodeDefaults = false,
            ignoreUnknownKeys = true
        )
    )
)
