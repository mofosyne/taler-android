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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.LinearLayoutManager
import net.taler.common.Amount
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.databinding.FragmentOrderStateBinding
import net.taler.merchantpos.order.OrderAdapter.OrderLineLookup

class OrderStateFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val orderManager by lazy { viewModel.orderManager }
    private val liveOrder by lazy { orderManager.getOrder(orderManager.currentOrderId.value!!) }

    private lateinit var ui: FragmentOrderStateBinding
    private val adapter = OrderAdapter()
    private var tracker: SelectionTracker<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentOrderStateBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.orderList.apply {
            adapter = this@OrderStateFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        val detailsLookup = OrderLineLookup(ui.orderList)
        val tracker = SelectionTracker.Builder(
            "order-selection-id",
            ui.orderList,
            adapter.keyProvider,
            detailsLookup,
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            SelectionPredicates.createSelectSingleAnything()
        ).build()
        savedInstanceState?.let { tracker.onRestoreInstanceState(it) }
        adapter.tracker = tracker
        this.tracker = tracker
        if (savedInstanceState == null) {
            // select last selected order line when re-creating this fragment
            // do it before attaching the tracker observer
            liveOrder.selectedProductKey?.let { tracker.select(it) }
        }
        tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onItemStateChanged(key: String, selected: Boolean) {
                super.onItemStateChanged(key, selected)
                val item = if (selected) adapter.getItemByKey(key) else null
                liveOrder.selectOrderLine(item)
            }
        })
        liveOrder.order.observe(viewLifecycleOwner) { order ->
            if (order == null) return@observe
            onOrderChanged(order, tracker)
        }
        liveOrder.orderTotal.observe(viewLifecycleOwner) { orderTotal: Amount ->
            if (orderTotal.isZero()) {
                ui.totalView.fadeOut()
                ui.totalView.text = null
            } else {
                ui.totalView.text = getString(R.string.order_total, orderTotal)
                ui.totalView.fadeIn()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        tracker?.onSaveInstanceState(outState)
    }

    private fun onOrderChanged(order: Order, tracker: SelectionTracker<String>) {
        adapter.setItems(order.products) {
            liveOrder.lastAddedProduct?.let {
                val position = adapter.findPosition(it)
                if (position >= 0) {
                    ui.orderList.scrollToPosition(position)
                    ui.orderList.post { this.tracker?.select(it.id) }
                }
            }
            // workaround for bug: SelectionObserver doesn't update when removing selected item
            if (tracker.hasSelection()) {
                val key = tracker.selection.first()
                val product = order.products.find { it.id == key }
                if (product == null) tracker.clearSelection()
            }
        }
    }

}
