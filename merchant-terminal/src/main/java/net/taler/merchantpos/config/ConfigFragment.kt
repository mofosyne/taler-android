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

package net.taler.merchantpos.config

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigFragmentDirections.Companion.actionSettingsToOrder
import net.taler.merchantpos.databinding.FragmentMerchantConfigBinding
import net.taler.merchantpos.topSnackbar

/**
 * Fragment that displays merchant settings.
 */
class ConfigFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val configManager by lazy { model.configManager }

    private lateinit var ui: FragmentMerchantConfigBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentMerchantConfigBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui.configUrlView.editText!!.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) checkForUrlCredentials()
        }
        ui.okButton.setOnClickListener {
            checkForUrlCredentials()
            val inputUrl = ui.configUrlView.editText!!.text
            val url = if (inputUrl.startsWith("http")) {
                inputUrl.toString()
            } else {
                "https://$inputUrl".also { ui.configUrlView.editText!!.setText(it) }
            }
            ui.progressBar.visibility = VISIBLE
            ui.okButton.visibility = INVISIBLE
            val config = Config(
                configUrl = url,
                username = ui.usernameView.editText!!.text.toString(),
                password = ui.passwordView.editText!!.text.toString()
            )
            configManager.fetchConfig(config, true, ui.savePasswordCheckBox.isChecked)
            configManager.configUpdateResult.observe(viewLifecycleOwner, Observer { result ->
                if (onConfigUpdate(result)) {
                    configManager.configUpdateResult.removeObservers(viewLifecycleOwner)
                }
            })
        }
        ui.forgetPasswordButton.setOnClickListener {
            configManager.forgetPassword()
            ui.passwordView.editText!!.text = null
            ui.forgetPasswordButton.visibility = GONE
        }
        ui.configDocsView.movementMethod = LinkMovementMethod.getInstance()
        updateView(savedInstanceState == null)
    }

    override fun onStart() {
        super.onStart()
        // focus password if this is the only empty field
        if (ui.passwordView.editText!!.text.isBlank()
            && !ui.configUrlView.editText!!.text.isBlank()
            && !ui.usernameView.editText!!.text.isBlank()
        ) {
            ui.passwordView.requestFocus()
        }
    }

    private fun updateView(isInitialization: Boolean = false) {
        val config = configManager.config
        ui.configUrlView.editText!!.setText(
            if (isInitialization && config.configUrl.isBlank()) CONFIG_URL_DEMO
            else config.configUrl
        )
        ui.usernameView.editText!!.setText(
            if (isInitialization && config.username.isBlank()) CONFIG_USERNAME_DEMO
            else config.username
        )
        ui.passwordView.editText!!.setText(
            if (isInitialization && config.password.isBlank()) CONFIG_PASSWORD_DEMO
            else config.password
        )
        ui.forgetPasswordButton.visibility = if (config.hasPassword()) VISIBLE else GONE
    }

    private fun checkForUrlCredentials() {
        val text = ui.configUrlView.editText!!.text.toString()
        Uri.parse(text)?.userInfo?.let { userInfo ->
            if (userInfo.contains(':')) {
                val (user, pass) = userInfo.split(':')
                val strippedUrl = text.replace("${userInfo}@", "")
                ui.configUrlView.editText!!.setText(strippedUrl)
                ui.usernameView.editText!!.setText(user)
                ui.passwordView.editText!!.setText(pass)
            }
        }
    }

    /**
     * Processes updated config and returns true, if observer can be removed.
     */
    private fun onConfigUpdate(result: ConfigUpdateResult?) = when (result) {
        null -> false
        is ConfigUpdateResult.Error -> {
            onError(result.msg)
            true
        }
        is ConfigUpdateResult.Success -> {
            onConfigReceived(result.currency)
            true
        }
    }

    private fun onConfigReceived(currency: String) {
        onResultReceived()
        updateView()
        topSnackbar(requireView(), getString(R.string.config_changed, currency), LENGTH_LONG)
        navigate(actionSettingsToOrder())
    }

    private fun onError(msg: String) {
        onResultReceived()
        Snackbar.make(requireView(), msg, LENGTH_LONG).show()
    }

    private fun onResultReceived() {
        ui.progressBar.visibility = INVISIBLE
        ui.okButton.visibility = VISIBLE
    }

}
