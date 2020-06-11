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

package net.taler.wallet.settings

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
import net.taler.common.toRelativeTime
import net.taler.wallet.BuildConfig.VERSION_CODE
import net.taler.wallet.BuildConfig.VERSION_NAME
import net.taler.wallet.BuildConfig.WALLET_CORE_VERSION
import net.taler.wallet.MainViewModel
import net.taler.wallet.R


class SettingsFragment : PreferenceFragmentCompat() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var prefBackup: Preference
    private lateinit var prefDevMode: SwitchPreferenceCompat
    private lateinit var prefWithdrawTest: Preference
    private lateinit var prefVersionApp: Preference
    private lateinit var prefVersionCore: Preference
    private lateinit var prefVersionExchange: Preference
    private lateinit var prefVersionMerchant: Preference
    private lateinit var prefReset: Preference
    private val devPrefs by lazy {
        listOf(
            prefBackup,
            prefWithdrawTest,
            prefVersionApp,
            prefVersionCore,
            prefVersionExchange,
            prefVersionMerchant,
            prefReset
        )
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)
        prefBackup = findPreference("pref_backup")!!
        prefDevMode = findPreference("pref_dev_mode")!!
        prefWithdrawTest = findPreference("pref_testkudos")!!
        prefVersionApp = findPreference("pref_version_app")!!
        prefVersionCore = findPreference("pref_version_core")!!
        prefVersionExchange = findPreference("pref_version_protocol_exchange")!!
        prefVersionMerchant = findPreference("pref_version_protocol_merchant")!!
        prefReset = findPreference("pref_reset")!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.lastBackup.observe(viewLifecycleOwner, Observer {
            val time = it.toRelativeTime(requireContext())
            prefBackup.summary = getString(R.string.backup_last, time)
        })

        model.devMode.observe(viewLifecycleOwner, Observer { enabled ->
            prefDevMode.isChecked = enabled
            if (enabled) {
                prefVersionApp.summary = "$VERSION_NAME ($VERSION_CODE)"
                prefVersionCore.summary = WALLET_CORE_VERSION
                model.exchangeVersion?.let { prefVersionExchange.summary = it }
                model.merchantVersion?.let { prefVersionMerchant.summary = it }
            }
            devPrefs.forEach { it.isVisible = enabled }
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
                Snackbar.make(requireView(), "Wallet has been reset", LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                Snackbar.make(requireView(), "Reset cancelled", LENGTH_SHORT).show()
            }
            .show()
    }

}
