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

import androidx.core.os.LocaleListCompat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import net.taler.merchantpos.Amount
import java.util.*
import java.util.Locale.LanguageRange
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

data class Category(
    val id: Int,
    val name: String,
    @JsonProperty("name_i18n")
    val nameI18n: Map<String, String>?
) {
    var selected: Boolean = false
    val localizedName: String get() = getLocalizedString(nameI18n, name)
}

@JsonInclude(NON_NULL)
abstract class Product {
    @get:JsonProperty("product_id")
    abstract val productId: String?
    abstract val description: String
    @get:JsonProperty("description_i18n")
    abstract val descriptionI18n: Map<String, String>?
    abstract val price: String
    @get:JsonProperty("delivery_location")
    abstract val location: String?
    abstract val image: String?
    @get:JsonIgnore
    val localizedDescription: String
        get() = getLocalizedString(descriptionI18n, description)
}

data class ConfigProduct(
    @JsonIgnore
    val id: String = UUID.randomUUID().toString(),
    override val productId: String?,
    override val description: String,
    override val descriptionI18n: Map<String, String>?,
    override val price: String,
    override val location: String?,
    override val image: String?,
    val categories: List<Int>,
    @JsonIgnore
    val quantity: Int = 0
) : Product() {
    val priceAsDouble by lazy { Amount.fromString(price).amount.toDouble() }

    override fun equals(other: Any?) = other is ConfigProduct && id == other.id
    override fun hashCode() = id.hashCode()
}

data class ContractProduct(
    override val productId: String?,
    override val description: String,
    override val descriptionI18n: Map<String, String>?,
    override val price: String,
    override val location: String?,
    override val image: String?,
    val quantity: Int
) : Product() {
    constructor(product: ConfigProduct) : this(
        product.productId,
        product.description,
        product.descriptionI18n,
        product.price,
        product.location,
        product.image,
        product.quantity
    )
}

private fun getLocalizedString(map: Map<String, String>?, default: String): String {
    // just return the default, if it is the only element
    if (map == null) return default
    // create a priority list of language ranges from system locales
    val locales = LocaleListCompat.getDefault()
    val priorityList = ArrayList<LanguageRange>(locales.size())
    for (i in 0 until locales.size()) {
        priorityList.add(LanguageRange(locales[i].toLanguageTag()))
    }
    // create a list of locales available in the given map
    val availableLocales = map.keys.mapNotNull {
        if (it == "_") return@mapNotNull null
        val list = it.split("_")
        when (list.size) {
            1 -> Locale(list[0])
            2 -> Locale(list[0], list[1])
            3 -> Locale(list[0], list[1], list[2])
            else -> null
        }
    }
    val match = Locale.lookup(priorityList, availableLocales)
    return match?.toString()?.let { map[it] } ?: default
}

data class Order(val id: Int, val availableCategories: Map<Int, Category>) {
    val products = ArrayList<ConfigProduct>()
    val title: String = id.toString()
    val summary: String
        get() {
            if (products.size == 1) return products[0].description
            return getCategoryQuantities().map { (category: Category, quantity: Int) ->
                "$quantity x ${category.localizedName}"
            }.joinToString()
        }
    val total: Double
        get() {
            var total = 0.0
            products.forEach { product ->
                val price = product.priceAsDouble
                total += price * product.quantity
            }
            return total
        }
    val totalAsString: String
        get() = String.format("%.2f", total)

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
            val category = availableCategories.getValue(categoryId)
            val oldQuantity = categories[category] ?: 0
            categories[category] = oldQuantity + product.quantity
        }
        return categories
    }

    /**
     * Returns a map of i18n summaries for each locale present in *all* given [Category]s
     * or null if there's no locale that fulfills this criteria.
     */
    val summaryI18n: Map<String, String>?
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

}
