/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

package net.taler.merchantpos.order

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import net.taler.common.Amount
import net.taler.common.AmountParserException
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigProduct
import net.taler.merchantpos.databinding.FragmentCustomDialogBinding

class CustomDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "CustomDialogFragment"
    }

    private val viewModel: MainViewModel by activityViewModels()

    private lateinit var ui: FragmentCustomDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentCustomDialogBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val currency = viewModel.configManager.currency ?: error("No currency")
        ui.currencyView.text = currency
        ui.addButton.setOnClickListener {
            val currentOrderId =
                viewModel.orderManager.currentOrderId.value ?: return@setOnClickListener
            val amount = try {
                Amount.fromString(currency, ui.amountLayout.editText!!.text.toString())
            } catch (e: AmountParserException) {
                Toast.makeText(requireContext(), R.string.refund_error_invalid_amount, LENGTH_LONG)
                    .show()
                return@setOnClickListener
            }
            val product = ConfigProduct(
                description = ui.productNameLayout.editText!!.text.toString(),
                price = amount,
                categories = listOf(Int.MIN_VALUE),
            )
            viewModel.orderManager.addProduct(currentOrderId, product)
            dismiss()
        }
        ui.cancelButton.setOnClickListener {
            dismiss()
        }
    }
}
