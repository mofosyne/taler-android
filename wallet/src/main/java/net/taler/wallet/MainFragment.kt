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

package net.taler.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.EventObserver
import net.taler.wallet.ScopeMode.MULTI
import net.taler.wallet.ScopeMode.SINGLE
import net.taler.wallet.balances.BalanceState
import net.taler.wallet.balances.BalanceState.Success
import net.taler.wallet.balances.BalancesFragment
import net.taler.wallet.databinding.FragmentMainBinding
import net.taler.wallet.transactions.TransactionsFragment

enum class ScopeMode { SINGLE, MULTI }

class MainFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private var scopeMode: ScopeMode? = null

    private lateinit var ui: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentMainBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.balanceManager.state.observe(viewLifecycleOwner) {
            onBalancesChanged(it)
        }
        model.transactionsEvent.observe(viewLifecycleOwner, EventObserver { scopeInfo ->
            // we only need to navigate to a dedicated list, when in multi-scope mode
            if (scopeMode == MULTI) {
                model.transactionManager.selectedScope = scopeInfo
                findNavController().navigate(R.id.action_nav_main_to_nav_transactions)
            }
        })

        ui.mainFab.setOnClickListener {
            model.scanCode()
        }
        ui.mainFab.setOnLongClickListener {
            findNavController().navigate(R.id.action_nav_main_to_nav_uri_input)
            true
        }
    }

    override fun onStart() {
        super.onStart()
        model.balanceManager.loadBalances()
    }

    private fun onBalancesChanged(state: BalanceState) {
        if (state !is Success) return
        val balances = state.balances
        val mode = if (balances.size == 1) SINGLE else MULTI
        if (scopeMode != mode) {
            val f = if (mode == SINGLE) {
                model.transactionManager.selectedScope = balances[0].scopeInfo
                TransactionsFragment()
            } else {
                BalancesFragment()
            }
            scopeMode = mode
            childFragmentManager.beginTransaction()
                .replace(R.id.mainFragmentContainer, f, mode.name)
                .commitNow()
        }
    }

}
