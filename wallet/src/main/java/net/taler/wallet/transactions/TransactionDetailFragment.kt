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
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.TAG
import net.taler.wallet.showError
import net.taler.wallet.transactions.TransactionAction.Abort
import net.taler.wallet.transactions.TransactionAction.Delete
import net.taler.wallet.transactions.TransactionAction.Fail
import net.taler.wallet.transactions.TransactionAction.Resume
import net.taler.wallet.transactions.TransactionAction.Retry
import net.taler.wallet.transactions.TransactionAction.Suspend

abstract class TransactionDetailFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    protected val transactionManager by lazy { model.transactionManager }
    protected val devMode get() = model.devMode.value == true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        transactionManager.selectedTransaction.observe(viewLifecycleOwner) {
            requireActivity().apply {
                it?.generalTitleRes?.let {
                    title = getString(it)
                }
            }
        }
    }

    private fun dialogTitle(t: TransactionAction): Int = when (t) {
        Delete -> R.string.transactions_delete_dialog_title
        Abort -> R.string.transactions_abort_dialog_title
        Fail -> R.string.transactions_fail_dialog_title
        else -> error("unsupported action: $t")
    }

    private fun dialogMessage(t: TransactionAction): Int = when (t) {
        Delete -> R.string.transactions_delete_dialog_message
        Abort -> R.string.transactions_abort_dialog_message
        Fail -> R.string.transactions_fail_dialog_message
        else -> error("unsupported action: $t")
    }

    private fun dialogButton(t: TransactionAction): Int = when (t) {
        Delete -> R.string.transactions_delete
        Abort -> R.string.transactions_abort
        Fail -> R.string.transactions_fail
        else -> error("unsupported")
    }

    protected fun onTransitionButtonClicked(t: Transaction, ta: TransactionAction) = when (ta) {
        Delete -> showDialog(ta) { deleteTransaction(t) }
        Abort -> showDialog(ta) { abortTransaction(t) }
        Fail -> showDialog(ta) { failTransaction(t) }
        Retry -> retryTransaction(t)
        Suspend -> suspendTransaction(t)
        Resume -> resumeTransaction(t)
    }

    private fun showDialog(tt: TransactionAction, onAction: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setTitle(dialogTitle(tt))
            .setMessage(dialogMessage(tt))
            .setNeutralButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setNegativeButton(dialogButton(tt)) { dialog, _ ->
                onAction()
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTransaction(t: Transaction) {
        transactionManager.deleteTransaction(t.transactionId) {
            Log.e(TAG, "Error deleteTransaction $it")
            showError(it)
        }
        findNavController().popBackStack()
    }

    private fun retryTransaction(t: Transaction) {
        transactionManager.retryTransaction(t.transactionId) {
            Log.e(TAG, "Error retryTransaction $it")
            showError(it)
        }
    }

    private fun abortTransaction(t: Transaction) {
        transactionManager.abortTransaction(t.transactionId) {
            Log.e(TAG, "Error abortTransaction $it")
            showError(it)
        }
    }

    private fun failTransaction(t: Transaction) {
        transactionManager.failTransaction(t.transactionId) {
            Log.e(TAG, "Error failTransaction $it")
            showError(it)
        }
    }

    private fun suspendTransaction(t: Transaction) {
        transactionManager.suspendTransaction(t.transactionId) {
            Log.e(TAG, "Error suspendTransaction $it")
            showError(it)
        }
    }

    private fun resumeTransaction(t: Transaction) {
        transactionManager.resumeTransaction(t.transactionId) {
            Log.e(TAG, "Error resumeTransaction $it")
            showError(it)
        }
    }
}
