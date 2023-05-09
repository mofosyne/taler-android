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

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import net.taler.common.Amount
import net.taler.common.CombinedLiveData
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.config.ConfigProduct
import net.taler.merchantpos.order.RestartState.DISABLED
import net.taler.merchantpos.order.RestartState.ENABLED
import net.taler.merchantpos.order.RestartState.UNDO

internal enum class RestartState { ENABLED, DISABLED, UNDO }

internal interface LiveOrder {
    val order: LiveData<Order?>
    val orderTotal: LiveData<Amount>
    val restartState: LiveData<RestartState>
    val modifyOrderAllowed: LiveData<Boolean>
    val lastAddedProduct: ConfigProduct?
    val selectedProductKey: String?
    fun restartOrUndo()
    fun selectOrderLine(product: ConfigProduct?)
    fun increaseSelectedOrderLine()
    fun decreaseSelectedOrderLine()
}

internal class MutableLiveOrder(
    val id: Int,
    private val currency: String,
    private val productsByCategory: HashMap<Category, ArrayList<ConfigProduct>>
) : LiveOrder {
    private val availableCategories: Map<Int, Category>
        get() = productsByCategory.keys.map { it.id to it }.toMap()
    override val order: MutableLiveData<Order?> =
        MutableLiveData(Order(id, currency, availableCategories))
    override val orderTotal: LiveData<Amount> = order.map { it?.total ?: Amount.zero(currency) }
    override val restartState = MutableLiveData(DISABLED)
    private val selectedOrderLine = MutableLiveData<ConfigProduct?>()
    override val selectedProductKey: String?
        get() = selectedOrderLine.value?.id
    override val modifyOrderAllowed =
        CombinedLiveData(restartState, selectedOrderLine) { restartState, selectedOrderLine ->
            restartState != DISABLED && selectedOrderLine != null
        }
    override var lastAddedProduct: ConfigProduct? = null
    private var undoOrder: Order? = null

    @UiThread
    internal fun addProduct(product: ConfigProduct) {
        lastAddedProduct = product
        order.value = order.value!! + product
        restartState.value = ENABLED
    }

    @UiThread
    internal fun removeProduct(product: ConfigProduct) {
        val modifiedOrder = order.value!! - product
        order.value = modifiedOrder
        restartState.value = if (modifiedOrder.products.isEmpty()) DISABLED else ENABLED
    }

    @UiThread
    internal fun isEmpty() = order.value!!.products.isEmpty()

    @UiThread
    override fun restartOrUndo() {
        if (restartState.value == UNDO) {
            order.value = undoOrder
            restartState.value = ENABLED
            undoOrder = null
        } else {
            undoOrder = order.value
            order.value = Order(id, currency, availableCategories)
            restartState.value = UNDO
        }
    }

    @UiThread
    override fun selectOrderLine(product: ConfigProduct?) {
        selectedOrderLine.value = product
    }

    @UiThread
    override fun increaseSelectedOrderLine() {
        val orderLine = selectedOrderLine.value ?: throw IllegalStateException()
        addProduct(orderLine)
    }

    @UiThread
    override fun decreaseSelectedOrderLine() {
        val orderLine = selectedOrderLine.value ?: throw IllegalStateException()
        removeProduct(orderLine)
    }

}
