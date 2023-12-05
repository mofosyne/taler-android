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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.EventObserver
import net.taler.wallet.CurrencyMode.MULTI
import net.taler.wallet.CurrencyMode.SINGLE
import net.taler.wallet.balances.BalanceState
import net.taler.wallet.balances.BalanceState.Error
import net.taler.wallet.balances.BalanceState.Loading
import net.taler.wallet.balances.BalanceState.None
import net.taler.wallet.balances.BalanceState.Success
import net.taler.wallet.balances.BalancesFragment
import net.taler.wallet.databinding.FragmentMainBinding
import net.taler.wallet.transactions.TransactionsFragment

enum class CurrencyMode { SINGLE, MULTI }

class MainFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private var currencyMode: CurrencyMode? = null

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
        model.balanceManager.balanceState.observe(viewLifecycleOwner) {
            onBalanceStateChanged(it)
        }
        model.transactionsEvent.observe(viewLifecycleOwner, EventObserver { currency ->
            // we only need to navigate to a dedicated list, when in multi-currency mode
            if (currencyMode == MULTI) {
                model.transactionManager.selectedCurrency = currency
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
        model.loadBalances()
    }

    private fun onBalanceStateChanged(state: BalanceState) {
        when (state) {
            is Loading -> {
                model.showProgressBar.value = true
            }
            is None -> {
                model.showProgressBar.value = false
            }
            is Success -> {
                model.showProgressBar.value = false
                val balances = state.balances
                val mode = if(balances.size == 1) SINGLE else MULTI
                val f = if (mode == SINGLE) {
                    model.transactionManager.selectedCurrency = balances[0].available.currency
                    TransactionsFragment()
                } else {
                    BalancesFragment()
                }
                currencyMode = mode
                childFragmentManager.beginTransaction()
                    .replace(R.id.mainFragmentContainer, f, mode.name)
                    .commitNow()
            }
            is Error -> {
                model.showProgressBar.value = false
                Log.e(TAG, "Error retrieving balances: ${state.error}")
            }
        }
    }

}
