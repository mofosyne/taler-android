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
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.fragment_main.*
import net.taler.wallet.balances.BalancesFragment
import net.taler.wallet.transactions.TransactionsFragment

class MainFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.balances.observe(viewLifecycleOwner, Observer {
            onBalancesChanged(it.values.toList())
        })

        mainFab.setOnClickListener {
            scanQrCode(requireActivity())
        }
    }

    override fun onStart() {
        super.onStart()
        model.loadBalances()
    }

    private fun onBalancesChanged(balances: List<BalanceItem>) {
        if (childFragmentManager.fragments.isEmpty()) {
            val f = if (balances.size == 1) {
                model.transactionManager.selectedCurrency = balances[0].available.currency
                TransactionsFragment()
            } else {
                BalancesFragment()
            }
            childFragmentManager.beginTransaction()
                .add(R.id.mainFragmentContainer, f)
                .commitNow()
        }
    }

}
