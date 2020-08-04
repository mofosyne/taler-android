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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import net.taler.merchantpos.R
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.order.CategoryAdapter.CategoryViewHolder

internal class CategoryAdapter(private val listener: CategorySelectionListener) :
    Adapter<CategoryViewHolder>() {

    private val categories = ArrayList<Category>()

    override fun getItemCount() = categories.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    fun setItems(items: List<Category>) {
        categories.clear()
        categories.addAll(items)
        notifyDataSetChanged()
    }

    internal inner class CategoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val button: Button = v.findViewById(R.id.button)

        fun bind(category: Category) {
            button.text = category.localizedName
            button.isPressed = category.selected
            button.setOnClickListener { listener.onCategorySelected(category) }
        }
    }

}
