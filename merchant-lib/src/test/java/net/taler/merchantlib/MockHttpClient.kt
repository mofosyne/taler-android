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
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.logging.SIMPLE
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.content.TextContent
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.http.hostWithPort
import org.junit.Assert.assertEquals

object MockHttpClient {

    val httpClient = HttpClient(MockEngine) {
        install(JsonFeature) {
            serializer = getSerializer()
        }
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.ALL
        }
        engine {
            addHandler { error("No response handler set") }
        }
    }

    fun HttpClient.giveJsonResponse(
        url: String,
        expectedBody: String? = null,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        jsonProducer: () -> String
    ) {
        val httpConfig = engineConfig as MockEngineConfig
        httpConfig.requestHandlers.removeAt(0)
        httpConfig.requestHandlers.add { request ->
            if (request.url.fullUrl == url) {
                val headers = headersOf("Content-Type" to listOf(Json.toString()))
                if (expectedBody != null) {
                    val content = request.body as TextContent
                    assertJsonEquals(expectedBody, content.text)
                }
                respond(jsonProducer(), headers = headers, status = statusCode)
            } else {
                error("Unexpected URL: ${request.url.fullUrl}")
            }
        }
    }

    private val Url.hostWithPortIfRequired: String get() = if (port == protocol.defaultPort) host else hostWithPort
    private val Url.fullUrl: String get() = "${protocol.name}://$hostWithPortIfRequired$fullPath"

    private fun assertJsonEquals(json1: String, json2: String) {
        val parsed1 = kotlinx.serialization.json.Json.parseJson(json1)
        val parsed2 = kotlinx.serialization.json.Json.parseJson(json2)
        assertEquals(parsed1, parsed2)
    }

}
