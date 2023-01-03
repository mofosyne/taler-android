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

package net.taler.cashier.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.cashier.MainViewModel
import net.taler.cashier.R
import net.taler.cashier.databinding.FragmentErrorBinding

class ErrorFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { viewModel.withdrawManager }

    private lateinit var ui: FragmentErrorBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentErrorBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        withdrawManager.withdrawStatus.observe(viewLifecycleOwner) { status ->
            if (status == null) return@observe
            if (status is WithdrawStatus.Aborted) {
                ui.textView.setText(R.string.transaction_aborted)
            } else if (status is WithdrawStatus.Error) {
                ui.textView.text = status.msg
            }
            withdrawManager.completeTransaction()
        }
        ui.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

}
