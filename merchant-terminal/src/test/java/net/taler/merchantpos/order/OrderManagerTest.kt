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

package net.taler.merchantpos.order

import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.runBlocking
import net.taler.merchantpos.R
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28]) // API 29 needs at least Java 9
@RunWith(AndroidJUnit4::class)
class OrderManagerTest {

    private val mapper = ObjectMapper()
        .registerModule(KotlinModule())
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val app: Application = getApplicationContext()
    private val orderManager = OrderManager(app, mapper)

    @Test
    fun `config test missing categories`() = runBlocking {
        val json = JSONObject(
            """
            { "categories": [] }
        """.trimIndent()
        )
        val result = orderManager.onConfigurationReceived(json, "KUDOS")
        assertEquals(app.getString(R.string.config_error_category), result)
    }

    @Test
    fun `config test currency mismatch`() = runBlocking {
        val json = JSONObject(
            """{
            "categories": [
                {
                    "id": 1,
                    "name": "Snacks"
                }
            ],
            "products": [
                {
                    "product_id": "631361561",
                    "description": "Chips",
                    "price": "WRONGCUR:1.00",
                    "categories": [ 1 ],
                    "delivery_location": "cafeteria"
                }
            ]
        }""".trimIndent()
        )
        val result = orderManager.onConfigurationReceived(json, "KUDOS")
        val expectedStr = app.getString(
            R.string.config_error_currency, "Chips", "WRONGCUR", "KUDOS"
        )
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test unknown category ID`() = runBlocking {
        val json = JSONObject(
            """{
            "categories": [
                {
                    "id": 1,
                    "name": "Snacks"
                }
            ],
            "products": [
                {
                    "product_id": "631361561",
                    "description": "Chips",
                    "price": "KUDOS:1.00",
                    "categories": [ 2 ]
                }
            ]
        }""".trimIndent()
        )
        val result = orderManager.onConfigurationReceived(json, "KUDOS")
        val expectedStr = app.getString(
            R.string.config_error_product_category_id, "Chips", 2
        )
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test no products`() = runBlocking {
        val json = JSONObject(
            """{
            "categories": [
                {
                    "id": 1,
                    "name": "Snacks"
                }
            ],
            "products": []
        }""".trimIndent()
        )
        val result = orderManager.onConfigurationReceived(json, "KUDOS")
        val expectedStr = app.getString(R.string.config_error_product_zero)
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test valid config gets accepted`() = runBlocking {
        val json = JSONObject(
            """{
            "categories": [
                {
                    "id": 1,
                    "name": "Snacks"
                }
            ],
            "products": [
                {
                    "product_id": "631361561",
                    "description": "Chips",
                    "price": "KUDOS:1.00",
                    "categories": [ 1 ]
                }
            ]
        }""".trimIndent()
        )
        val result = orderManager.onConfigurationReceived(json, "KUDOS")
        assertNull(result)
    }

}
