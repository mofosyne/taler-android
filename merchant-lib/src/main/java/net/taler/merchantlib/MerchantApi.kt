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

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import net.taler.common.ContractTerms
import net.taler.merchantlib.Response.Companion.response

class MerchantApi(private val httpClient: HttpClient) {

    suspend fun getConfig(baseUrl: String): ConfigResponse {
        return httpClient.get("$baseUrl/config")
    }

    suspend fun postOrder(
        merchantConfig: MerchantConfig,
        contractTerms: ContractTerms
    ): Response<PostOrderResponse> = response {
        httpClient.post(merchantConfig.urlFor("private/orders")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
            contentType(Json)
            body = PostOrderRequest(contractTerms)
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
    ): Response<HttpResponse> = response {
        val resp = httpClient.delete(merchantConfig.urlFor("private/orders/$orderId")) {
            header(Authorization, "ApiKey ${merchantConfig.apiKey}")
        } as HttpResponse
        // TODO remove when the API call was fixed
        Log.e("TEST", "status: ${resp.status.value}")
        Log.e("TEST", String(resp.readBytes()))
        resp
    }

}

fun getDefaultHttpClient(): HttpClient = HttpClient(OkHttp) {
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
