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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar


class SettingsFragment : PreferenceFragmentCompat() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var prefDevMode: SwitchPreferenceCompat
    private lateinit var prefWithdrawTest: Preference
    private lateinit var prefReset: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)
        prefDevMode = findPreference("pref_dev_mode")!!
        prefWithdrawTest = findPreference("pref_testkudos")!!
        prefReset = findPreference("pref_reset")!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.devMode.observe(viewLifecycleOwner, Observer { enabled ->
            prefDevMode.isChecked = enabled
            prefWithdrawTest.isVisible = enabled
            prefReset.isVisible = enabled
        })
        prefDevMode.setOnPreferenceChangeListener { _, newValue ->
            model.devMode.value = newValue as Boolean
            true
        }

        withdrawManager.testWithdrawalInProgress.observe(viewLifecycleOwner, Observer { loading ->
            prefWithdrawTest.isEnabled = !loading
            model.showProgressBar.value = loading
        })
        prefWithdrawTest.setOnPreferenceClickListener {
            withdrawManager.withdrawTestkudos()
            true
        }

        prefReset.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Do you really want to reset the wallet and lose all coins and purchases?")
            .setPositiveButton("Reset") { _, _ ->
                model.dangerouslyReset()
                Snackbar.make(view!!, "Wallet has been reset", LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Snackbar.make(view!!, "Reset cancelled", LENGTH_SHORT).show()
            }
            .show()
    }

}
