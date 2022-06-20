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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import net.taler.cashier.BuildConfig.VERSION_NAME
import net.taler.cashier.config.VERSION_BANK
import net.taler.cashier.databinding.FragmentAboutDialogBinding
import net.taler.common.Version

class AboutDialogFragment : DialogFragment() {

    private lateinit var ui: FragmentAboutDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentAboutDialogBinding.inflate(layoutInflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.versionView.text = getString(R.string.about_version, VERSION_NAME)
        ui.bankVersionView.text = getString(R.string.about_supported_bank_api, VERSION_BANK.str())
        ui.licenseView.text =
            getString(R.string.about_license, getString(R.string.about_license_content))
        ui.copyrightView.text =
            getString(R.string.about_copyright, getString(R.string.about_copyright_holder))

        ui.button.setOnClickListener { dismiss() }
    }

    private fun Version.str(): String {
        return "$current:$revision:$age"
    }

}