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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.config.Category
import net.taler.merchantpos.databinding.FragmentCategoriesBinding

interface CategorySelectionListener {
    fun onCategorySelected(category: Category)
}

class CategoriesFragment : Fragment(), CategorySelectionListener {

    private val viewModel: MainViewModel by activityViewModels()
    private val orderManager by lazy { viewModel.orderManager }

    private lateinit var ui: FragmentCategoriesBinding
    private val adapter = CategoryAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentCategoriesBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.categoriesList.apply {
            adapter = this@CategoriesFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        orderManager.categories.observe(viewLifecycleOwner) { categories ->
            adapter.setItems(categories)
            ui.progressBar.visibility = INVISIBLE
        }
    }

    override fun onCategorySelected(category: Category) {
        orderManager.setCurrentCategory(category)
    }

}
