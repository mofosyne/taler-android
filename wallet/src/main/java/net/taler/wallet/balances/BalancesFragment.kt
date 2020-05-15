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
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import kotlinx.android.synthetic.main.fragment_balances.*
import net.taler.common.fadeIn
import net.taler.wallet.MainViewModel
import net.taler.wallet.R

interface BalanceClickListener {
    fun onBalanceClick(currency: String)
}

class BalancesFragment : Fragment(),
    BalanceClickListener {

    private val model: MainViewModel by activityViewModels()

    private val balancesAdapter = BalanceAdapter(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_balances, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainList.apply {
            adapter = balancesAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        model.balances.observe(viewLifecycleOwner, Observer {
            onBalancesChanged(it.values.toList())
        })
    }

    private fun onBalancesChanged(balances: List<BalanceItem>) {
        beginDelayedTransition(view as ViewGroup)
        if (balances.isEmpty()) {
            mainEmptyState.visibility = VISIBLE
            mainList.visibility = GONE
        } else {
            balancesAdapter.setItems(balances)
            mainEmptyState.visibility = INVISIBLE
            mainList.fadeIn()
        }
    }

    override fun onBalanceClick(currency: String) {
        model.showTransactions(currency)
    }

}
