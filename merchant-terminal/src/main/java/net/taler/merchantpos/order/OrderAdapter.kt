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

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigProduct
import net.taler.merchantpos.order.OrderAdapter.OrderViewHolder

internal class OrderAdapter : Adapter<OrderViewHolder>() {

    lateinit var tracker: SelectionTracker<String>
    val keyProvider = OrderKeyProvider()
    private val itemCallback = object : ItemCallback<ConfigProduct>() {
        override fun areItemsTheSame(oldItem: ConfigProduct, newItem: ConfigProduct): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ConfigProduct, newItem: ConfigProduct): Boolean {
            return oldItem.quantity == newItem.quantity
        }
    }
    private val differ = AsyncListDiffer(this, itemCallback)

    override fun getItemCount() = differ.currentList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val item = getItem(position)!!
        holder.bind(item, tracker.isSelected(item.id))
    }

    fun setItems(items: List<ConfigProduct>, commitCallback: () -> Unit) {
        // toMutableList() is needed for some reason, otherwise doesn't update adapter
        differ.submitList(items.toMutableList(), commitCallback)
    }

    fun getItem(position: Int): ConfigProduct? = differ.currentList[position]

    fun getItemByKey(key: String): ConfigProduct? {
        return differ.currentList.find { it.id == key }
    }

    fun findPosition(product: ConfigProduct): Int {
        return differ.currentList.indexOf(product)
    }

    internal inner class OrderViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
        private val quantity: TextView = v.findViewById(R.id.quantity)
        private val name: TextView = v.findViewById(R.id.name)
        private val price: TextView = v.findViewById(R.id.price)

        fun bind(product: ConfigProduct, selected: Boolean) {
            v.isActivated = selected
            quantity.text = product.quantity.toString()
            name.text = product.localizedDescription
            price.text = product.totalPrice.amountStr
        }
    }

    internal inner class OrderKeyProvider : ItemKeyProvider<String>(SCOPE_MAPPED) {
        override fun getKey(position: Int) = getItem(position)!!.id
        override fun getPosition(key: String): Int {
            return differ.currentList.indexOfFirst { it.id == key }
        }
    }

    internal class OrderLineLookup(private val list: RecyclerView) : ItemDetailsLookup<String>() {
        override fun getItemDetails(e: MotionEvent): ItemDetails<String>? {
            list.findChildViewUnder(e.x, e.y)?.let { view ->
                val holder = list.getChildViewHolder(view)
                val adapter = list.adapter as OrderAdapter
                val position = holder.adapterPosition
                return object : ItemDetails<String>() {
                    override fun getPosition(): Int = position
                    override fun getSelectionKey(): String = adapter.keyProvider.getKey(position)
                    override fun inSelectionHotspot(e: MotionEvent) = true
                }
            }
            return null
        }
    }

}