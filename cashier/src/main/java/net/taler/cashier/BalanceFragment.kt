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

package net.taler.cashier

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_balance.*
import net.taler.cashier.BalanceFragmentDirections.Companion.actionBalanceFragmentToTransactionFragment
import net.taler.cashier.withdraw.LastTransaction
import net.taler.cashier.withdraw.WithdrawStatus
import net.taler.common.Amount
import net.taler.common.SignedAmount
import net.taler.common.exhaustive
import net.taler.common.fadeIn
import net.taler.common.fadeOut

sealed class BalanceResult {
    class Error(val msg: String) : BalanceResult()
    object Offline : BalanceResult()
    class Success(val amount: SignedAmount) : BalanceResult()
}

class BalanceFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { viewModel.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withdrawManager.lastTransaction.observe(viewLifecycleOwner, Observer { lastTransaction ->
            onLastTransaction(lastTransaction)
        })
        viewModel.balance.observe(viewLifecycleOwner, Observer { result ->
            onBalanceUpdated(result)
        })
        button5.setOnClickListener { onAmountButtonPressed(5) }
        button10.setOnClickListener { onAmountButtonPressed(10) }
        button20.setOnClickListener { onAmountButtonPressed(20) }
        button50.setOnClickListener { onAmountButtonPressed(50) }

        if (savedInstanceState != null) {
            amountView.editText!!.setText(savedInstanceState.getCharSequence("amountView"))
        }
        amountView.editText!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onAmountConfirmed(getAmountFromView())
                true
            } else false
        }
        viewModel.currency.observe(viewLifecycleOwner, Observer { currency ->
            currencyView.text = currency
        })
        confirmWithdrawalButton.setOnClickListener { onAmountConfirmed(getAmountFromView()) }
    }

    override fun onStart() {
        super.onStart()
        // update balance if there's a config
        if (viewModel.hasConfig()) {
            viewModel.getBalance()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // for some reason automatic restore isn't working at the moment!?
        amountView?.editText?.text.let {
            outState.putCharSequence("amountView", it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.balance, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_reconfigure -> {
            findNavController().navigate(viewModel.configDestination)
            true
        }
        R.id.action_lock -> {
            viewModel.lock()
            findNavController().navigate(viewModel.configDestination)
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onBalanceUpdated(result: BalanceResult) {
        val uiList = listOf(
            introView,
            button5, button10, button20, button50,
            amountView, currencyView, confirmWithdrawalButton
        )
        when (result) {
            is BalanceResult.Success -> {
                balanceView.text = result.amount.toString()
                uiList.forEach { it.fadeIn() }
            }
            is BalanceResult.Error -> {
                balanceView.text = getString(R.string.balance_error, result.msg)
                uiList.forEach { it.fadeOut() }
            }
            BalanceResult.Offline -> {
                balanceView.text = getString(R.string.balance_offline)
                uiList.forEach { it.fadeOut() }
            }
        }.exhaustive
        progressBar.fadeOut()
    }

    private fun onAmountButtonPressed(amount: Int) {
        amountView.editText!!.setText(amount.toString())
        amountView.error = null
    }

    private fun getAmountFromView(): Amount {
        val str = amountView.editText!!.text.toString()
        val currency = viewModel.currency.value!!
        if (str.isBlank()) return Amount.zero(currency)
        return Amount.fromString(currency, str)
    }

    private fun onAmountConfirmed(amount: Amount) {
        if (amount.isZero()) {
            amountView.error = getString(R.string.withdraw_error_zero)
        } else if (!withdrawManager.hasSufficientBalance(amount)) {
            amountView.error = getString(R.string.withdraw_error_insufficient_balance)
        } else {
            amountView.error = null
            withdrawManager.withdraw(amount)
            actionBalanceFragmentToTransactionFragment().let {
                findNavController().navigate(it)
            }
        }
    }

    private fun onLastTransaction(lastTransaction: LastTransaction?) {
        val status = lastTransaction?.withdrawStatus
        val text = when (status) {
            is WithdrawStatus.Success -> getString(
                R.string.transaction_last_success, lastTransaction.withdrawAmount
            )
            is WithdrawStatus.Aborted -> getString(R.string.transaction_last_aborted)
            else -> getString(R.string.transaction_last_error)
        }
        lastTransactionView.text = text
        val drawable = if (status == WithdrawStatus.Success)
            R.drawable.ic_check_circle
        else
            R.drawable.ic_error
        lastTransactionView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        lastTransactionView.visibility = VISIBLE
    }

}
