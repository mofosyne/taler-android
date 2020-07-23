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
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager.beginDelayedTransition
import kotlinx.android.synthetic.main.fragment_order.*
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.order.OrderFragmentDirections.Companion.actionGlobalConfigFetcher
import net.taler.merchantpos.order.OrderFragmentDirections.Companion.actionOrderToMerchantSettings
import net.taler.merchantpos.order.OrderFragmentDirections.Companion.actionOrderToProcessPayment
import net.taler.merchantpos.order.RestartState.ENABLED
import net.taler.merchantpos.order.RestartState.UNDO

class OrderFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val orderManager by lazy { viewModel.orderManager }
    private val paymentManager by lazy { viewModel.paymentManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        orderManager.currentOrderId.observe(viewLifecycleOwner, Observer { orderId ->
            val liveOrder = orderManager.getOrder(orderId)
            onOrderSwitched(orderId, liveOrder)
            // add a new OrderStateFragment for each order
            // as switching its internals (like we do here) would be too messy
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment1, OrderStateFragment())
                .commit()
        })
    }

    override fun onStart() {
        super.onStart()
        if (!viewModel.configManager.config.isValid()) {
            navigate(actionOrderToMerchantSettings())
        } else if (viewModel.configManager.currency == null) {
            navigate(actionGlobalConfigFetcher())
        }
    }

    private fun onOrderSwitched(orderId: Int, liveOrder: LiveOrder) {
        // order title
        liveOrder.order.observe(viewLifecycleOwner, Observer { order ->
            activity?.title = getString(R.string.order_label_title, order.title)
        })
        // restart button
        restartButton.setOnClickListener { liveOrder.restartOrUndo() }
        liveOrder.restartState.observe(viewLifecycleOwner, Observer { state ->
            beginDelayedTransition(view as ViewGroup)
            if (state == UNDO) {
                restartButton.setText(R.string.order_undo)
                restartButton.isEnabled = true
                completeButton.isEnabled = false
            } else {
                restartButton.setText(R.string.order_restart)
                restartButton.isEnabled = state == ENABLED
                completeButton.isEnabled = state == ENABLED
            }
        })
        // -1 and +1 buttons
        liveOrder.modifyOrderAllowed.observe(viewLifecycleOwner, Observer { allowed ->
            minusButton.isEnabled = allowed
            plusButton.isEnabled = allowed
        })
        minusButton.setOnClickListener { liveOrder.decreaseSelectedOrderLine() }
        plusButton.setOnClickListener { liveOrder.increaseSelectedOrderLine() }
        // previous and next button
        prevButton.isEnabled = orderManager.hasPreviousOrder(orderId)
        orderManager.hasNextOrder(orderId).observe(viewLifecycleOwner, Observer { hasNextOrder ->
            nextButton.isEnabled = hasNextOrder
        })
        prevButton.setOnClickListener { orderManager.previousOrder() }
        nextButton.setOnClickListener { orderManager.nextOrder() }
        // complete button
        completeButton.setOnClickListener {
            val order = liveOrder.order.value ?: return@setOnClickListener
            paymentManager.createPayment(order)
            navigate(actionOrderToProcessPayment())
        }
    }

}
