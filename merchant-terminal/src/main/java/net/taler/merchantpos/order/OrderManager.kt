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

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations.map
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.taler.merchantpos.Amount.Companion.fromString
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigurationReceiver
import net.taler.merchantpos.order.RestartState.ENABLED
import org.json.JSONObject

class OrderManager(
    private val context: Context,
    private val mapper: ObjectMapper
) : ConfigurationReceiver {

    companion object {
        val TAG = OrderManager::class.java.simpleName
    }

    private var orderCounter: Int = 0
    private val mCurrentOrderId = MutableLiveData<Int>()
    internal val currentOrderId: LiveData<Int> = mCurrentOrderId

    private val productsByCategory = HashMap<Category, ArrayList<ConfigProduct>>()

    private val orders = LinkedHashMap<Int, MutableLiveOrder>()

    private val mProducts = MutableLiveData<List<ConfigProduct>>()
    internal val products: LiveData<List<ConfigProduct>> = mProducts

    private val mCategories = MutableLiveData<List<Category>>()
    internal val categories: LiveData<List<Category>> = mCategories

    override suspend fun onConfigurationReceived(json: JSONObject, currency: String): String? {
        // parse categories
        val categoriesStr = json.getJSONArray("categories").toString()
        val categoriesType = object : TypeReference<List<Category>>() {}
        val categories: List<Category> = mapper.readValue(categoriesStr, categoriesType)
        if (categories.isEmpty()) {
            Log.e(TAG, "No valid category found.")
            return context.getString(R.string.config_error_category)
        }
        // pre-select the first category
        categories[0].selected = true

        // parse products (live data gets updated in setCurrentCategory())
        val productsStr = json.getJSONArray("products").toString()
        val productsType = object : TypeReference<List<ConfigProduct>>() {}
        val products: List<ConfigProduct> = mapper.readValue(productsStr, productsType)

        // group products by categories
        productsByCategory.clear()
        products.forEach { product ->
            val productCurrency = fromString(product.price).currency
            if (productCurrency != currency) {
                Log.e(TAG, "Product $product has currency $productCurrency, $currency expected")
                return context.getString(
                    R.string.config_error_currency, product.description, productCurrency, currency
                )
            }
            product.categories.forEach { categoryId ->
                val category = categories.find { it.id == categoryId }
                if (category == null) {
                    Log.e(TAG, "Product $product has unknown category $categoryId")
                    return context.getString(
                        R.string.config_error_product_category_id, product.description, categoryId
                    )
                }
                if (productsByCategory.containsKey(category)) {
                    productsByCategory[category]?.add(product)
                } else {
                    productsByCategory[category] = ArrayList<ConfigProduct>().apply { add(product) }
                }
            }
        }
        return if (productsByCategory.size > 0) {
            mCategories.postValue(categories)
            mProducts.postValue(productsByCategory[categories[0]])
            // Initialize first empty order, note this won't work when updating config mid-flight
            if (orders.isEmpty()) {
                val id = orderCounter++
                orders[id] = MutableLiveOrder(id, productsByCategory)
                mCurrentOrderId.postValue(id)
            }
            null // success, no error string
        } else context.getString(R.string.config_error_product_zero)
    }

    @UiThread
    internal fun getOrder(orderId: Int): LiveOrder {
        return orders[orderId] ?: throw IllegalArgumentException()
    }

    @UiThread
    internal fun nextOrder() {
        val currentId = currentOrderId.value!!
        var foundCurrentOrder = false
        var nextId: Int? = null
        for (orderId in orders.keys) {
            if (foundCurrentOrder) {
                nextId = orderId
                break
            }
            if (orderId == currentId) foundCurrentOrder = true
        }
        if (nextId == null) {
            nextId = orderCounter++
            orders[nextId] = MutableLiveOrder(nextId, productsByCategory)
        }
        val currentOrder = order(currentId)
        if (currentOrder.isEmpty()) orders.remove(currentId)
        else currentOrder.lastAddedProduct = null  // not needed anymore and it would get selected
        mCurrentOrderId.value = nextId
    }

    @UiThread
    internal fun previousOrder() {
        val currentId = currentOrderId.value!!
        var previousId: Int? = null
        var foundCurrentOrder = false
        for (orderId in orders.keys) {
            if (orderId == currentId) {
                foundCurrentOrder = true
                break
            }
            previousId = orderId
        }
        if (previousId == null || !foundCurrentOrder) {
            throw AssertionError("Could not find previous order for $currentId")
        }
        val currentOrder = order(currentId)
        // remove current order if empty, or lastAddedProduct as it is not needed anymore
        // and would get selected when navigating back instead of last selection
        if (currentOrder.isEmpty()) orders.remove(currentId)
        else currentOrder.lastAddedProduct = null
        mCurrentOrderId.value = previousId
    }

    fun hasPreviousOrder(currentOrderId: Int): Boolean {
        return currentOrderId != orders.keys.first()
    }

    fun hasNextOrder(currentOrderId: Int) = map(order(currentOrderId).restartState) { state ->
        state == ENABLED || currentOrderId != orders.keys.last()
    }

    internal fun setCurrentCategory(category: Category) {
        val newCategories = categories.value?.apply {
            forEach { if (it.selected) it.selected = false }
            category.selected = true
        }
        mCategories.postValue(newCategories)
        mProducts.postValue(productsByCategory[category])
    }

    @UiThread
    internal fun addProduct(orderId: Int, product: ConfigProduct) {
        order(orderId).addProduct(product)
    }

    @UiThread
    internal fun onOrderPaid(orderId: Int) {
        if (currentOrderId.value == orderId) {
            if (hasPreviousOrder(orderId)) previousOrder()
            else nextOrder()
        }
        orders.remove(orderId)
    }

    private fun order(orderId: Int): MutableLiveOrder {
        return orders[orderId] ?: throw IllegalStateException()
    }

}
