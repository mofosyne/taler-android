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

import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import net.taler.wallet.databinding.FragmentUriInputBinding

class UriInputFragment : Fragment() {

    private lateinit var ui: FragmentUriInputBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = FragmentUriInputBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val clipboard = requireContext().getSystemService<ClipboardManager>()

        ui.pasteButton.setOnClickListener {
            val item = clipboard?.primaryClip?.getItemAt(0)
            if (item?.text != null) {
                ui.uriView.setText(item.text)
            } else {
                if (item?.uri != null) {
                    ui.uriView.setText(item.uri.toString())
                } else {
                    Toast.makeText(requireContext(), R.string.paste_invalid, LENGTH_LONG).show()
                }
            }
        }
        ui.okButton.setOnClickListener {
            val trimmedText = ui.uriView.text?.trim()
            if (trimmedText?.startsWith("taler://", ignoreCase = true) == true ||
                trimmedText?.startsWith("payto://", ignoreCase = true) == true) {
                ui.uriLayout.error = null
                launchInAppBrowser(requireContext(), trimmedText.toString())
            } else {
                ui.uriLayout.error = getString(R.string.uri_invalid)
            }
        }
    }

}
