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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import net.taler.common.showError
import net.taler.common.toRelativeTime
import net.taler.wallet.BuildConfig.FLAVOR
import net.taler.wallet.BuildConfig.VERSION_CODE
import net.taler.wallet.BuildConfig.VERSION_NAME
import net.taler.wallet.BuildConfig.WALLET_CORE_VERSION
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.withdraw.WithdrawTestStatus


class SettingsFragment : PreferenceFragmentCompat() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var prefBackup: Preference
    private lateinit var prefDevMode: SwitchPreferenceCompat
    private lateinit var prefWithdrawTest: Preference
    private lateinit var prefLogcat: Preference
    private lateinit var prefVersionApp: Preference
    private lateinit var prefVersionCore: Preference
    private lateinit var prefVersionExchange: Preference
    private lateinit var prefVersionMerchant: Preference
    private lateinit var prefReset: Preference
    private val devPrefs by lazy {
        listOf(
            prefBackup,
            prefWithdrawTest,
            prefLogcat,
            prefVersionApp,
            prefVersionCore,
            prefVersionExchange,
            prefVersionMerchant,
            prefReset
        )
    }

    val createDocumentActivity =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->

        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_main, rootKey)
        prefBackup = findPreference("pref_backup")!!
        prefDevMode = findPreference("pref_dev_mode")!!
        prefWithdrawTest = findPreference("pref_testkudos")!!
        prefLogcat = findPreference("pref_logcat")!!
        prefVersionApp = findPreference("pref_version_app")!!
        prefVersionCore = findPreference("pref_version_core")!!
        prefVersionExchange = findPreference("pref_version_protocol_exchange")!!
        prefVersionMerchant = findPreference("pref_version_protocol_merchant")!!
        prefReset = findPreference("pref_reset")!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        model.lastBackup.observe(viewLifecycleOwner) {
            val time = it.toRelativeTime(requireContext())
            prefBackup.summary = getString(R.string.backup_last, time)
        }

        model.devMode.observe(viewLifecycleOwner) { enabled ->
            prefDevMode.isChecked = enabled
            if (enabled) {
                prefVersionApp.summary = "$VERSION_NAME ($FLAVOR $VERSION_CODE)"
                prefVersionCore.summary = WALLET_CORE_VERSION
                model.exchangeVersion?.let { prefVersionExchange.summary = it }
                model.merchantVersion?.let { prefVersionMerchant.summary = it }
            }
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
            val toast =
                Toast.makeText(requireActivity(), "Log export currently unavailable", Toast.LENGTH_LONG)
            toast.show()

//            val myPid = android.os.Process.myPid()
//            val proc = Runtime.getRuntime()
//                .exec(arrayOf("logcat", "-d", "--pid=$myPid", "*:V"))
//            val bytes = proc.inputStream.readBytes()
//            val f = File(requireActivity().getExternalFilesDir(null),
//                "taler-wallet-log-${System.currentTimeMillis()}.txt")
//            f.writeBytes(bytes)
//            val toast = Toast.makeText(requireActivity(), "Saved to ${f.absolutePath}", Toast.LENGTH_LONG)
//            toast.show()
//            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
//                addCategory(Intent.CATEGORY_OPENABLE)
//                type = "application/pdf"
//                putExtra(Intent.EXTRA_TITLE, "invoice.pdf")
//
//                // Optionally, specify a URI for the directory that should be opened in
//                // the system file picker before your app creates the document.
//                putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
//            }
//            startActivityForResult(intent, CREATE_FILE)
//            ActivityResultContracts.CreateDocument
            true
        }

        prefReset.setOnPreferenceClickListener {
            showResetDialog()
            true
        }
    }

    private fun showResetDialog() {
        AlertDialog.Builder(requireContext(), R.style.DialogTheme)
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
