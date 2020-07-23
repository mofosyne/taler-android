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
import kotlinx.coroutines.runBlocking
import net.taler.common.Amount
import net.taler.merchantlib.MerchantConfig
import net.taler.merchantpos.R
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.config.ConfigProduct
import net.taler.merchantpos.config.PosConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [28]) // API 29 needs at least Java 9
@RunWith(AndroidJUnit4::class)
class OrderManagerTest {

    private val app: Application = getApplicationContext()
    private val orderManager = OrderManager(app)
    private val posConfig = PosConfig(
        merchantConfig = MerchantConfig(
            baseUrl = "http://example.org",
            apiKey = "sandbox"
        ),
        categories = listOf(
            Category(1, "one"),
            Category(2, "two")
        ),
        products = listOf(
            ConfigProduct(
                description = "foo",
                price = Amount("KUDOS", 1, 0),
                categories = listOf(1)
            ),
            ConfigProduct(
                description = "bar",
                price = Amount("KUDOS", 1, 50000),
                categories = listOf(2)
            )
        )
    )

    @Test
    fun `config test missing categories`() = runBlocking {
        val config = posConfig.copy(categories = emptyList())
        val result = orderManager.onConfigurationReceived(config, "KUDOS")
        assertEquals(app.getString(R.string.config_error_category), result)
    }

    @Test
    fun `config test currency mismatch`() = runBlocking {
        val products = listOf(posConfig.products[0].copy(price = Amount("WRONGCUR", 1, 0)))
        val config = posConfig.copy(products = products)
        val result = orderManager.onConfigurationReceived(config, "KUDOS")
        val expectedStr = app.getString(
            R.string.config_error_currency, "foo", "WRONGCUR", "KUDOS"
        )
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test unknown category ID`() = runBlocking {
        val products = listOf(posConfig.products[0].copy(categories = listOf(42)))
        val config = posConfig.copy(products = products)
        val result = orderManager.onConfigurationReceived(config, "KUDOS")
        val expectedStr = app.getString(
            R.string.config_error_product_category_id, "foo", 42
        )
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test no products`() = runBlocking {
        val config = posConfig.copy(products = emptyList())
        val result = orderManager.onConfigurationReceived(config, "KUDOS")
        val expectedStr = app.getString(R.string.config_error_product_zero)
        assertEquals(expectedStr, result)
    }

    @Test
    fun `config test valid config gets accepted`() = runBlocking {
        val result = orderManager.onConfigurationReceived(posConfig, "KUDOS")
        assertNull(result)
    }

}
