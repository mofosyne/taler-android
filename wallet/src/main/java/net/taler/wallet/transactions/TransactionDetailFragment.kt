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

package net.taler.wallet.transactions

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.transactions.TransactionAction.*

abstract class TransactionDetailFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    val transactionManager by lazy { model.transactionManager }
    val devMode by lazy { model.devMode }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(model.devMode.value == true)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        transactionManager.selectedTransaction.observe(viewLifecycleOwner) {
            requireActivity().apply {
                it?.generalTitleRes?.let {
                    title = getString(it)
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.transactions_detail, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun dialogTitle(t: TransactionAction): Int? = when (t) {
        Delete -> R.string.transactions_delete_dialog_title
        Abort -> R.string.transactions_abort_dialog_title
        else -> null
    }

    private fun dialogMessage(t: TransactionAction): Int? = when (t) {
        Delete -> R.string.transactions_delete_dialog_message
        Abort -> R.string.transactions_abort_dialog_message
        else -> null
    }

    private fun dialogButton(t: TransactionAction): Int? = when (t) {
        Delete -> R.string.transactions_delete
        Abort -> R.string.transactions_abort
        else -> null
    }

    protected fun onTransitionButton(t: Transaction, tt: TransactionAction) {
        when (tt) {
            Delete, Abort -> {
                MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
                    .setTitle(dialogTitle(tt)!!)
                    .setMessage(dialogMessage(tt)!!)
                    .setNeutralButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setNegativeButton(dialogButton(tt)!!) { dialog, _ ->
                        when (tt) {
                            Delete -> deleteTransaction(t)
                            Abort -> abortTransaction(t)
                            else -> {}
                        }
                        dialog.dismiss()
                    }
                    .show()
            }
            else -> when (tt) {
                Retry -> retryTransaction(t)
                Suspend -> suspendTransaction(t)
                Resume -> resumeTransaction(t)
                else -> {}
            }
        }
    }

    private fun deleteTransaction(t: Transaction) {
        transactionManager.deleteTransaction(t.transactionId)
        findNavController().popBackStack()
    }

    private fun retryTransaction(t: Transaction) {
        transactionManager.retryTransaction(t.transactionId)
    }

    private fun abortTransaction(t: Transaction) {
        transactionManager.abortTransaction(t.transactionId)
    }

    private fun suspendTransaction(t: Transaction) {
        transactionManager.suspendTransaction(t.transactionId)
    }

    private fun resumeTransaction(t: Transaction) {
        transactionManager.resumeTransaction(t.transactionId)
    }
}
