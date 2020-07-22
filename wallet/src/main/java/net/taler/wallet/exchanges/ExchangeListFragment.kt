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
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_exchange_list.*
import net.taler.common.EventObserver
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

class ExchangeListFragment : Fragment(), ExchangeClickListener {

    private val model: MainViewModel by activityViewModels()
    private val exchangeManager by lazy { model.exchangeManager }
    private val exchangeAdapter by lazy { ExchangeAdapter(this) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_exchange_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        list.apply {
            adapter = exchangeAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }
        addExchangeFab.setOnClickListener {
            AddExchangeDialogFragment().show(parentFragmentManager, "ADD_EXCHANGE")
        }

        exchangeManager.progress.observe(viewLifecycleOwner, Observer { show ->
            if (show) progressBar.fadeIn() else progressBar.fadeOut()
        })
        exchangeManager.exchanges.observe(viewLifecycleOwner, Observer { exchanges ->
            onExchangeUpdate(exchanges)
        })
        exchangeManager.addError.observe(viewLifecycleOwner, EventObserver { error ->
            if (error) onAddExchangeFailed()
        })
    }

    private fun onExchangeUpdate(exchanges: List<ExchangeItem>) {
        exchangeAdapter.update(exchanges)
        if (exchanges.isEmpty()) {
            emptyState.fadeIn()
            list.fadeOut()
        } else {
            emptyState.fadeOut()
            list.fadeIn()
        }
    }

    private fun onAddExchangeFailed() {
        Toast.makeText(requireContext(), R.string.exchange_add_error, LENGTH_LONG).show()
    }

    override fun onManualWithdraw(item: ExchangeItem) {
        exchangeManager.withdrawalExchange = item
        findNavController().navigate(R.id.action_nav_settings_exchanges_to_nav_exchange_manual_withdrawal)
    }

}