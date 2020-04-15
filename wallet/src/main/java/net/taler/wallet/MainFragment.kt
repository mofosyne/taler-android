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
import android.transition.TransitionManager.beginDelayedTransition
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager.VERTICAL
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentIntegrator.QR_CODE
import kotlinx.android.synthetic.main.fragment_show_balance.*

interface BalanceClickListener {
    fun onBalanceClick()
}

class MainFragment : Fragment(), BalanceClickListener {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private var reloadBalanceMenuItem: MenuItem? = null
    private val balancesAdapter = BalanceAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_show_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = balancesAdapter
            addItemDecoration(DividerItemDecoration(context, VERTICAL))
        }

        model.balances.observe(viewLifecycleOwner, Observer {
            onBalancesChanged(it)
        })

//        model.devMode.observe(viewLifecycleOwner, Observer { enabled ->
//            delayedTransition()
//            testWithdrawButton.visibility = if (enabled) VISIBLE else GONE
//            reloadBalanceMenuItem?.isVisible = enabled
//        })
//        testWithdrawButton.setOnClickListener {
//            withdrawManager.withdrawTestkudos()
//        }
//        withdrawManager.testWithdrawalInProgress.observe(viewLifecycleOwner, Observer { loading ->
//            Log.v("taler-wallet", "observing balance loading $loading in show balance")
//            testWithdrawButton.isEnabled = !loading
//            model.showProgressBar.value = loading
//        })

        mainFab.setOnClickListener {
            onScanButtonClicked()
        }
    }

    override fun onStart() {
        super.onStart()
        model.loadBalances()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.reload_balance -> {
                model.loadBalances()
                true
            }
            R.id.developer_mode -> {
                item.isChecked = !item.isChecked
                model.devMode.value = item.isChecked
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.balance, menu)
        menu.findItem(R.id.developer_mode).isChecked = model.devMode.value!!
        reloadBalanceMenuItem = menu.findItem(R.id.reload_balance).apply {
            isVisible = model.devMode.value!!
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    private fun onScanButtonClicked() {
        IntentIntegrator(activity).apply {
            setPrompt("")
            setBeepEnabled(true)
            setOrientationLocked(false)
        }.initiateScan(listOf(QR_CODE))
    }

    private fun onBalancesChanged(balances: List<BalanceItem>) {
        delayedTransition()
        if (balances.isEmpty()) {
            mainEmptyState.visibility = VISIBLE
            mainList.visibility = GONE
        } else {
            balancesAdapter.setItems(balances)
            mainEmptyState.visibility = GONE
            mainList.visibility = VISIBLE
        }
    }

    private fun delayedTransition() {
        beginDelayedTransition(view as ViewGroup)
    }

    override fun onBalanceClick() {
        findNavController().navigate(R.id.walletHistory)
    }

}
