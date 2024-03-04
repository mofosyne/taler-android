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
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import net.taler.common.showError
import net.taler.wallet.BuildConfig.FLAVOR
import net.taler.wallet.BuildConfig.VERSION_CODE
import net.taler.wallet.BuildConfig.VERSION_NAME
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.withdraw.WithdrawTestStatus
import java.lang.System.currentTimeMillis


class SettingsFragment : PreferenceFragmentCompat() {

    private val model: MainViewModel by activityViewModels()
    private val settingsManager get() = model.settingsManager
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var prefDevMode: SwitchPreferenceCompat
    private lateinit var prefWithdrawTest: Preference
    private lateinit var prefLogcat: Preference
    private lateinit var prefExportDb: Preference
    private lateinit var prefImportDb: Preference
    private lateinit var prefVersionApp: Preference
    private lateinit var prefVersionCore: Preference
    private lateinit var prefVersionExchange: Preference
    private lateinit var prefVersionMerchant: Preference
    private lateinit var prefTest: Preference
    private lateinit var prefReset: Preference
    private val devPrefs by lazy {
        listOf(
            prefVersionCore,
            prefWithdrawTest,
            prefLogcat,
            prefExportDb,
            prefImportDb,
            prefVersionExchange,
            prefVersionMerchant,
            prefTest,
            prefReset,
        )
    }

    private val logLauncher = registerForActivityResult(CreateDocument("text/plain")) { uri ->
        settingsManager.exportLogcat(uri)
    }
    private val dbExportLauncher =
        registerForActivityResult(CreateDocument("application/json")) { uri ->
            settingsManager.exportDb(uri)
        }
    private val dbImportLauncher =
        registerForActivityResult(OpenDocument()) { uri ->
            settingsManager.importDb(uri)
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)
        prefDevMode = findPreference("pref_dev_mode")!!
        prefWithdrawTest = findPreference("pref_testkudos")!!
        prefLogcat = findPreference("pref_logcat")!!
        prefExportDb = findPreference("pref_export_db")!!
        prefImportDb = findPreference("pref_import_db")!!
        prefVersionApp = findPreference("pref_version_app")!!
        prefVersionCore = findPreference("pref_version_core")!!
        prefVersionExchange = findPreference("pref_version_protocol_exchange")!!
        prefVersionMerchant = findPreference("pref_version_protocol_merchant")!!
        prefTest = findPreference("pref_test")!!
        prefReset = findPreference("pref_reset")!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefVersionApp.summary = "$VERSION_NAME ($FLAVOR $VERSION_CODE)"
        prefVersionCore.summary = "${model.walletVersion} (${model.walletVersionHash?.take(7)})"
        model.exchangeVersion?.let { prefVersionExchange.summary = it }
        model.merchantVersion?.let { prefVersionMerchant.summary = it }

        model.devMode.observe(viewLifecycleOwner) { enabled ->
            prefDevMode.isChecked = enabled
            devPrefs.forEach { it.isVisible = enabled }
        }
        prefDevMode.setOnPreferenceChangeListener { _, newValue ->
            model.devMode.value = newValue as Boolean
            true
        }

        withdrawManager.testWithdrawalStatus.observe(viewLifecycleOwner) { status ->
            if (status == null) return@observe
            val loading = status is WithdrawTestStatus.Withdrawing
            prefWithdrawTest.isEnabled = !loading
            model.showProgressBar.value = loading
            if (status is WithdrawTestStatus.Error) {
                requireActivity().showError(R.string.withdraw_error_test, status.message)
            }
            withdrawManager.testWithdrawalStatus.value = null
        }
        prefWithdrawTest.setOnPreferenceClickListener {
            withdrawManager.withdrawTestkudos()
            true
        }

        prefLogcat.setOnPreferenceClickListener {
            logLauncher.launch("taler-wallet-log-${currentTimeMillis()}.txt")
            true
        }
        prefExportDb.setOnPreferenceClickListener {
            dbExportLauncher.launch("taler-wallet-db-${currentTimeMillis()}.json")
            true
        }
        prefImportDb.setOnPreferenceClickListener {
            showImportDialog()
            true
        }
        prefTest.setOnPreferenceClickListener {
            model.runIntegrationTest()
            true
        }
        prefReset.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showImportDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setMessage(R.string.settings_dialog_import_message)
            .setNegativeButton(R.string.import_db) { _, _ ->
                dbImportLauncher.launch(arrayOf("application/json"))
            }
            .setPositiveButton(R.string.cancel) { _, _ ->
                Snackbar.make(requireView(), getString(R.string.settings_alert_import_canceled), LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Material3)
            .setMessage(R.string.settings_dialog_reset_message)
            .setNegativeButton(R.string.reset) { _, _ ->
                settingsManager.clearDb {
                    model.dangerouslyReset()
                }
                Snackbar.make(requireView(), getString(R.string.settings_alert_reset_done), LENGTH_SHORT).show()
            }
            .setPositiveButton(R.string.cancel) { _, _ ->
                Snackbar.make(requireView(), getString(R.string.settings_alert_reset_canceled), LENGTH_SHORT).show()
            }
            .show()
    }
}
