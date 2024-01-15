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

package net.taler.wallet.balances

import android.os.Bundle
import android.transition.TransitionManager.beginDelayedTransition
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import net.taler.common.fadeIn
import net.taler.wallet.MainViewModel
import net.taler.wallet.balances.BalanceState.Error
import net.taler.wallet.balances.BalanceState.Loading
import net.taler.wallet.balances.BalanceState.None
import net.taler.wallet.balances.BalanceState.Success
import net.taler.wallet.databinding.FragmentBalancesBinding
import net.taler.wallet.showError

interface BalanceClickListener {
    fun onBalanceClick(currency: String)
}

class BalancesFragment : Fragment(),
    BalanceClickListener {

    private val model: MainViewModel by activityViewModels()

    private lateinit var ui: FragmentBalancesBinding
    private val balancesAdapter = BalanceAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = FragmentBalancesBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.mainList.apply {
            adapter = balancesAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        model.balanceManager.state.observe(viewLifecycleOwner) {
            onBalancesChanged(it)
        }
    }

    private fun onBalancesChanged(state: BalanceState) {
        model.showProgressBar.value = false
        when (state) {
            is None -> {}
            is Loading -> {
                model.showProgressBar.value = true
            }
            is Success -> {
                beginDelayedTransition(view as ViewGroup)
                if (state.balances.isEmpty()) {
                    ui.mainEmptyState.visibility = VISIBLE
                    ui.mainList.visibility = GONE
                } else {
                    balancesAdapter.setItems(state.balances)
                    ui.mainEmptyState.visibility = INVISIBLE
                    ui.mainList.fadeIn()
                }
            }
            is Error -> showError(state.error)
        }
    }

    override fun onBalanceClick(currency: String) {
        model.showTransactions(currency)
    }

}
