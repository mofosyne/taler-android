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
import androidx.navigation.fragment.findNavController
import net.taler.cashier.BalanceFragmentDirections.Companion.actionBalanceFragmentToTransactionFragment
import net.taler.cashier.databinding.FragmentBalanceBinding
import net.taler.cashier.withdraw.LastTransaction
import net.taler.cashier.withdraw.WithdrawStatus
import net.taler.common.Amount
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
    private val configManager by lazy { viewModel.configManager }
    private val withdrawManager by lazy { viewModel.withdrawManager }

    private lateinit var ui: FragmentBalanceBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        setHasOptionsMenu(true)
        ui = FragmentBalanceBinding.inflate(layoutInflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withdrawManager.lastTransaction.observe(viewLifecycleOwner, { lastTransaction ->
            onLastTransaction(lastTransaction)
        })
        viewModel.balance.observe(viewLifecycleOwner, { result ->
            onBalanceUpdated(result)
        })
        ui.button5.setOnClickListener { onAmountButtonPressed(5) }
        ui.button10.setOnClickListener { onAmountButtonPressed(10) }
        ui.button20.setOnClickListener { onAmountButtonPressed(20) }
        ui.button50.setOnClickListener { onAmountButtonPressed(50) }

        if (savedInstanceState != null) {
            ui.amountView.editText!!.setText(savedInstanceState.getCharSequence("amountView"))
        }
        ui.amountView.editText!!.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onAmountConfirmed(getAmountFromView())
                true
            } else false
        }
        configManager.currency.observe(viewLifecycleOwner, { currency ->
            ui.currencyView.text = currency
        })
        ui.confirmWithdrawalButton.setOnClickListener { onAmountConfirmed(getAmountFromView()) }
    }

    override fun onStart() {
        super.onStart()
        // update balance if there's a config
        if (configManager.hasConfig()) {
            viewModel.getBalance()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // automatic restore isn't working, maybe because of the different layout in landscape mode
        // the ui won't be available after onDestroyView()
        if (view != null) ui.amountView.editText?.text.let {
            outState.putCharSequence("amountView", it)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.balance, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_reconfigure -> {
            findNavController().navigate(configManager.configDestination)
            true
        }
        R.id.action_lock -> {
            viewModel.lock()
            findNavController().navigate(configManager.configDestination)
            true
        }
        R.id.action_about -> {
            AboutDialogFragment().show(parentFragmentManager, "ABOUT")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onBalanceUpdated(result: BalanceResult) {
        val uiList = listOf(
            ui.introView,
            ui.button5, ui.button10, ui.button20, ui.button50,
            ui.amountView, ui.currencyView, ui.confirmWithdrawalButton
        )
        when (result) {
            is BalanceResult.Success -> {
                ui.balanceView.text = result.amount.toString()
                uiList.forEach { it.fadeIn() }
            }
            is BalanceResult.Error -> {
                ui.balanceView.text = getString(R.string.balance_error, result.msg)
                uiList.forEach { it.fadeOut() }
            }
            BalanceResult.Offline -> {
                ui.balanceView.text = getString(R.string.balance_offline)
                uiList.forEach { it.fadeOut() }
            }
        }.exhaustive
        ui.progressBar.fadeOut()
    }

    private fun onAmountButtonPressed(amount: Int) {
        ui.amountView.editText!!.setText(amount.toString())
        ui.amountView.error = null
    }

    private fun getAmountFromView(): Amount {
        val str = ui.amountView.editText!!.text.toString()
        val currency = configManager.currency.value!!
        if (str.isBlank()) return Amount.zero(currency)
        return Amount.fromString(currency, str)
    }

    private fun onAmountConfirmed(amount: Amount) {
        if (amount.isZero()) {
            ui.amountView.error = getString(R.string.withdraw_error_zero)
        } else when (withdrawManager.hasSufficientBalance(amount)) {
            true -> {
                ui.amountView.error = null
                withdrawManager.withdraw(amount)
                actionBalanceFragmentToTransactionFragment().let {
                    findNavController().navigate(it)
                }
            }
            false -> {
                ui.amountView.error = getString(R.string.withdraw_error_insufficient_balance)
            }
            null -> {
                ui.amountView.error = getString(R.string.withdraw_error_currency_mismatch)
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
        ui.lastTransactionView.text = text
        val drawable = if (status == WithdrawStatus.Success)
            R.drawable.ic_check_circle
        else
            R.drawable.ic_error
        ui.lastTransactionView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, 0, 0, 0)
        ui.lastTransactionView.visibility = VISIBLE
    }

}
