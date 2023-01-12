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

package net.taler.wallet.exchanges

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.taler.wallet.MainViewModel
import net.taler.wallet.R


class AddExchangeDialogFragment : DialogFragment() {

    private val model: MainViewModel by activityViewModels()
    private val exchangeManager by lazy { model.exchangeManager }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setIcon(R.drawable.ic_account_balance)
            .setTitle(R.string.exchange_list_add)
            .setView(R.layout.dialog_exchange_add)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                val urlView: TextView = (dialog as AlertDialog).findViewById(R.id.urlView)!!
                exchangeManager.add(urlView.text.toString())
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                dismiss()
            }
            .create()
    }

}
