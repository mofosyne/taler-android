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
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import kotlinx.android.synthetic.main.fragment_categories.*
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.order.CategoryAdapter.CategoryViewHolder

interface CategorySelectionListener {
    fun onCategorySelected(category: Category)
}

class CategoriesFragment : Fragment(), CategorySelectionListener {

    private val viewModel: MainViewModel by activityViewModels()
    private val orderManager by lazy { viewModel.orderManager }
    private val adapter = CategoryAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        categoriesList.apply {
            adapter = this@CategoriesFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        orderManager.categories.observe(viewLifecycleOwner, Observer { categories ->
            adapter.setItems(categories)
            progressBar.visibility = INVISIBLE
        })
    }

    override fun onCategorySelected(category: Category) {
        orderManager.setCurrentCategory(category)
    }

}

private class CategoryAdapter(
    private val listener: CategorySelectionListener
) : Adapter<CategoryViewHolder>() {

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

    private inner class CategoryViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val button: Button = v.findViewById(R.id.button)

        fun bind(category: Category) {
            button.text = category.localizedName
            button.isPressed = category.selected
            button.setOnClickListener { listener.onCategorySelected(category) }
        }
    }

}
