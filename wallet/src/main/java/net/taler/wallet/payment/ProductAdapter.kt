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

package net.taler.wallet.payment

import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeByteArray
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.taler.common.ContractProduct
import net.taler.wallet.R
import net.taler.wallet.payment.ProductAdapter.ProductViewHolder

internal interface ProductImageClickListener {
    fun onImageClick(image: Bitmap)
}

internal class ProductAdapter(private val listener: ProductImageClickListener) :
    RecyclerView.Adapter<ProductViewHolder>() {

    private val items = ArrayList<ContractProduct>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun setItems(items: List<ContractProduct>) {
        this.items.clear()
        this.items.addAll(items)
        notifyDataSetChanged()
    }

    internal inner class ProductViewHolder(v: View) : ViewHolder(v) {
        private val quantity: TextView = v.findViewById(R.id.quantity)
        private val image: ImageView = v.findViewById(R.id.image)
        private val name: TextView = v.findViewById(R.id.name)
        private val price: TextView = v.findViewById(R.id.price)

        fun bind(product: ContractProduct) {
            quantity.text = product.quantity.toString()
            val productImage = product.image
            if (productImage == null) {
                image.visibility = GONE
            } else REGEX_PRODUCT_IMAGE.matchEntire(productImage)?.let { match ->
                match.groups[2]?.value?.let { group ->
                    image.visibility = VISIBLE
                    val decodedString = Base64.decode(group, Base64.DEFAULT)
                    val bitmap = decodeByteArray(decodedString, 0, decodedString.size)
                    image.setImageBitmap(bitmap)
                    image.setOnClickListener {
                        listener.onImageClick(bitmap)
                    }
                }
            }
            name.text = product.description
            price.visibility = product.totalPrice?.let {
                price.text = it.toString()
                VISIBLE
            } ?: GONE
        }
    }

}
