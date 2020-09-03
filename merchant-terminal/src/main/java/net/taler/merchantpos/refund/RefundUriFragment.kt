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

package net.taler.merchantpos.refund

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import net.taler.common.NfcManager.Companion.hasNfc
import net.taler.common.QrCodeManager.makeQrCode
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.databinding.FragmentRefundUriBinding

class RefundUriFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val refundManager by lazy { model.refundManager }

    private lateinit var ui: FragmentRefundUriBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentRefundUriBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = refundManager.refundResult.value
        if (result !is RefundResult.Success) throw IllegalStateException()

        ui.refundQrcodeView.setImageBitmap(makeQrCode(result.refundUri))

        val introRes =
            if (hasNfc(requireContext())) R.string.refund_intro_nfc else R.string.refund_intro
        ui.refundIntroView.setText(introRes)

        ui.refundAmountView.text = result.amount.toString()

        ui.refundRefView.text =
            getString(R.string.refund_order_ref, result.item.orderId, result.reason)

        ui.cancelRefundButton.setOnClickListener { findNavController().navigateUp() }
        ui.completeButton.setOnClickListener { findNavController().navigateUp() }
    }

    override fun onDestroy() {
        super.onDestroy()
        refundManager.abortRefund()
    }

}
