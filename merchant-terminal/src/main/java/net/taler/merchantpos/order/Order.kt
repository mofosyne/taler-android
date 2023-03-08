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

import net.taler.common.Amount
import net.taler.common.ContractTerms
import net.taler.common.Timestamp
import net.taler.common.now
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.config.ConfigProduct
import java.net.URLEncoder
import java.util.concurrent.TimeUnit.HOURS

private const val FULFILLMENT_PREFIX = "taler://fulfillment-success/"

data class Order(val id: Int, val currency: String, val availableCategories: Map<Int, Category>) {
    val products = ArrayList<ConfigProduct>()
    val title: String = id.toString()
    val summary: String
        get() {
            if (products.size == 1) return products[0].description
            return getCategoryQuantities().map { (category: Category, quantity: Int) ->
                "$quantity x ${category.localizedName}"
            }.joinToString()
        }
    val total: Amount
        get() {
            var total = Amount.zero(currency)
            products.forEach { product ->
                total += product.price * product.quantity
            }
            return total
        }

    operator fun plus(product: ConfigProduct): Order {
        val i = products.indexOf(product)
        if (i == -1) {
            products.add(product.copy(quantity = 1))
        } else {
            val quantity = products[i].quantity
            products[i] = products[i].copy(quantity = quantity + 1)
        }
        return this
    }

    operator fun minus(product: ConfigProduct): Order {
        val i = products.indexOf(product)
        if (i == -1) return this
        val quantity = products[i].quantity
        if (quantity <= 1) {
            products.remove(product)
        } else {
            products[i] = products[i].copy(quantity = quantity - 1)
        }
        return this
    }

    private fun getCategoryQuantities(): HashMap<Category, Int> {
        val categories = HashMap<Category, Int>()
        products.forEach { product ->
            val categoryId = product.categories[0]
            val category = availableCategories[categoryId] ?: return@forEach // custom products
            val oldQuantity = categories[category] ?: 0
            categories[category] = oldQuantity + product.quantity
        }
        return categories
    }

    /**
     * Returns a map of i18n summaries for each locale present in *all* given [Category]s
     * or null if there's no locale that fulfills this criteria.
     */
    private val summaryI18n: Map<String, String>?
        get() {
            if (products.size == 1) return products[0].descriptionI18n
            val categoryQuantities = getCategoryQuantities()
            // get all available locales
            val availableLocales = categoryQuantities.mapNotNull { (category, _) ->
                val nameI18n = category.nameI18n
                // if one category doesn't have locales, we can return null here already
                nameI18n?.keys ?: return null
            }.flatten().toHashSet()
            // remove all locales not supported by all categories
            categoryQuantities.forEach { (category, _) ->
                // category.nameI18n should be non-null now
                availableLocales.retainAll(category.nameI18n!!.keys)
                if (availableLocales.isEmpty()) return null
            }
            return availableLocales.map { locale ->
                Pair(
                    locale, categoryQuantities.map { (category, quantity) ->
                        // category.nameI18n should be non-null now
                        "$quantity x ${category.nameI18n!![locale]}"
                    }.joinToString()
                )
            }.toMap()
        }

    private val fulfillmentUri: String
        get() {
            val fulfillmentId = "${now()}-${hashCode()}"
            return "$FULFILLMENT_PREFIX${URLEncoder.encode(summary, "UTF-8")}#$fulfillmentId"
        }

    fun toContractTerms(): ContractTerms {
        val deadline = Timestamp.fromMillis(now() + HOURS.toMillis(1))
        return ContractTerms(
            summary = summary,
            summaryI18n = summaryI18n,
            amount = total,
            fulfillmentUrl = fulfillmentUri,
            products = products.map { it.toContractProduct() },
            refundDeadline = deadline,
            wireTransferDeadline = deadline
        )
    }

}
