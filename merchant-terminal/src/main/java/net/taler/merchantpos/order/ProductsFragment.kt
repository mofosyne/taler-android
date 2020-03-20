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
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.synthetic.main.fragment_products.*
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigProduct
import net.taler.merchantpos.order.ProductAdapter.ProductViewHolder

interface ProductSelectionListener {
    fun onProductSelected(product: ConfigProduct)
}

class ProductsFragment : Fragment(), ProductSelectionListener {

    private val viewModel: MainViewModel by activityViewModels()
    private val orderManager by lazy { viewModel.orderManager }
    private val adapter = ProductAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        productsList.apply {
            adapter = this@ProductsFragment.adapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }

        orderManager.products.observe(viewLifecycleOwner, Observer { products ->
            if (products == null) {
                adapter.setItems(emptyList())
            } else {
                adapter.setItems(products)
            }
            progressBar.visibility = INVISIBLE
        })
    }

    override fun onProductSelected(product: ConfigProduct) {
        orderManager.addProduct(orderManager.currentOrderId.value!!, product)
    }

}

private class ProductAdapter(
    private val listener: ProductSelectionListener
) : Adapter<ProductViewHolder>() {

    private val products = ArrayList<ConfigProduct>()

    override fun getItemCount() = products.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    fun setItems(items: List<ConfigProduct>) {
        products.clear()
        products.addAll(items)
        notifyDataSetChanged()
    }

    private inner class ProductViewHolder(private val v: View) : ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.name)
        private val price: TextView = v.findViewById(R.id.price)

        fun bind(product: ConfigProduct) {
            name.text = product.localizedDescription
            price.text = product.price.amountStr
            v.setOnClickListener { listener.onProductSelected(product) }
        }
    }

}
