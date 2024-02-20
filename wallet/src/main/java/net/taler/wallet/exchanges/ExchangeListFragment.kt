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

package net.taler.wallet.exchanges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.taler.common.EventObserver
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.common.showError
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentExchangeListBinding
import net.taler.wallet.showError

open class ExchangeListFragment : Fragment(), ExchangeClickListener {

    protected val model: MainViewModel by activityViewModels()
    private val exchangeManager by lazy { model.exchangeManager }
    private val transactionManager get() = model.transactionManager

    protected lateinit var ui: FragmentExchangeListBinding
    protected open val isSelectOnly = false
    private val exchangeAdapter by lazy { ExchangeAdapter(isSelectOnly, this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        ui = FragmentExchangeListBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                if (model.devMode.value == true) {
                    menuInflater.inflate(R.menu.exchange_list, menu)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_add_dev_exchanges) {
                    exchangeManager.addDevExchanges()
                }
                return true
            }
        }, viewLifecycleOwner, RESUMED)

        ui.list.apply {
            adapter = exchangeAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        ui.addExchangeFab.setOnClickListener {
            AddExchangeDialogFragment().show(parentFragmentManager, "ADD_EXCHANGE")
        }

        exchangeManager.progress.observe(viewLifecycleOwner) { show ->
            if (show) ui.progressBar.fadeIn() else ui.progressBar.fadeOut()
        }
        exchangeManager.exchanges.observe(viewLifecycleOwner) { exchanges ->
            onExchangeUpdate(exchanges)
        }
        exchangeManager.addError.observe(viewLifecycleOwner, EventObserver { error ->
            onAddExchangeFailed()
            if (model.devMode.value == true) {
                showError(error)
            }
        })
        exchangeManager.listError.observe(viewLifecycleOwner, EventObserver { error ->
            onListExchangeFailed()
            if (model.devMode.value == true) {
                showError(error)
            }
        })
        exchangeManager.deleteError.observe(viewLifecycleOwner, EventObserver { error ->
            if (model.devMode.value == true) {
                showError(error)
            } else {
                showError(error.userFacingMsg)
            }
        })
    }

    protected open fun onExchangeUpdate(exchanges: List<ExchangeItem>) {
        exchangeAdapter.update(exchanges)
        if (exchanges.isEmpty()) {
            ui.emptyState.fadeIn()
            ui.list.fadeOut()
        } else {
            ui.emptyState.fadeOut()
            ui.list.fadeIn()
        }
    }

    private fun onAddExchangeFailed() {
        Toast.makeText(requireContext(), R.string.exchange_add_error, LENGTH_LONG).show()
    }

    private fun onListExchangeFailed() {
        Toast.makeText(requireContext(), R.string.exchange_list_error, LENGTH_LONG).show()
    }

    override fun onExchangeSelected(item: ExchangeItem) {
        throw AssertionError("must not get triggered here")
    }

    override fun onManualWithdraw(item: ExchangeItem) {
        exchangeManager.withdrawalExchange = item
        findNavController().navigate(R.id.action_nav_settings_exchanges_to_nav_exchange_manual_withdrawal)
    }

    override fun onPeerReceive(item: ExchangeItem) {
        transactionManager.selectedScope = item.scopeInfo
        findNavController().navigate(R.id.action_global_receiveFunds)
    }

    override fun onExchangeDelete(item: ExchangeItem) {
        val optionsArray = arrayOf(getString(R.string.exchange_delete_force))
        val checkedArray = BooleanArray(1) { false }

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setTitle(R.string.exchange_delete)
            .setMultiChoiceItems(optionsArray, checkedArray) { _, which, isChecked ->
                checkedArray[which] = isChecked
            }
            .setNegativeButton(R.string.transactions_delete) { _, _ ->
                exchangeManager.delete(item.exchangeBaseUrl, checkedArray[0])
            }
            .setPositiveButton(R.string.cancel) { _, _ -> }
            .show()
    }
}
