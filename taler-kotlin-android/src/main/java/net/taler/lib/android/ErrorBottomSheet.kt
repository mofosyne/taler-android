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

package net.taler.lib.android

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.taler.common.R
import net.taler.common.databinding.BottomsheetErrorBinding

class ErrorBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(mainText: String, detailText: String) = ErrorBottomSheet().apply {
            arguments = Bundle().apply {
                putString("TEXT_MAIN", mainText)
                putString("TEXT_DETAIL", detailText)
            }
            setStyle(STYLE_NORMAL, R.style.ErrorBottomSheetTheme)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val ui = BottomsheetErrorBinding.inflate(inflater, container, false)
        val args = requireArguments()
        val mainText = args.getString("TEXT_MAIN")
        val detailText = args.getString("TEXT_DETAIL")
        ui.mainText.text = mainText
        ui.closeButton.setOnClickListener { dismiss() }
        ui.detailText.text = detailText
        ui.shareButton.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$mainText\n\n$detailText")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
        return ui.root
    }

}
