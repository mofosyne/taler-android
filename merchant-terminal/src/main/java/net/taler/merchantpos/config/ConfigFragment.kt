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
import kotlinx.android.synthetic.main.fragment_merchant_config.*
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigFragmentDirections.Companion.actionSettingsToOrder
import net.taler.merchantpos.topSnackbar

/**
 * Fragment that displays merchant settings.
 */
class ConfigFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val configManager by lazy { model.configManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_merchant_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        configUrlView.editText!!.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) checkForUrlCredentials()
        }
        okButton.setOnClickListener {
            checkForUrlCredentials()
            val inputUrl = configUrlView.editText!!.text
            val url = if (inputUrl.startsWith("http")) {
                inputUrl.toString()
            } else {
                "https://$inputUrl".also { configUrlView.editText!!.setText(it) }
            }
            progressBar.visibility = VISIBLE
            okButton.visibility = INVISIBLE
            val config = Config(
                configUrl = url,
                username = usernameView.editText!!.text.toString(),
                password = passwordView.editText!!.text.toString()
            )
            configManager.fetchConfig(config, true, savePasswordCheckBox.isChecked)
            configManager.configUpdateResult.observe(viewLifecycleOwner, Observer { result ->
                if (onConfigUpdate(result)) {
                    configManager.configUpdateResult.removeObservers(viewLifecycleOwner)
                }
            })
        }
        forgetPasswordButton.setOnClickListener {
            configManager.forgetPassword()
            passwordView.editText!!.text = null
            forgetPasswordButton.visibility = GONE
        }
        configDocsView.movementMethod = LinkMovementMethod.getInstance()
        updateView(savedInstanceState == null)
    }

    override fun onStart() {
        super.onStart()
        // focus password if this is the only empty field
        if (passwordView.editText!!.text.isBlank()
            && !configUrlView.editText!!.text.isBlank()
            && !usernameView.editText!!.text.isBlank()
        ) {
            passwordView.requestFocus()
        }
    }

    private fun updateView(isInitialization: Boolean = false) {
        val config = configManager.config
        configUrlView.editText!!.setText(
            if (isInitialization && config.configUrl.isBlank()) CONFIG_URL_DEMO
            else config.configUrl
        )
        usernameView.editText!!.setText(
            if (isInitialization && config.username.isBlank()) CONFIG_USERNAME_DEMO
            else config.username
        )
        passwordView.editText!!.setText(
            if (isInitialization && config.password.isBlank()) CONFIG_PASSWORD_DEMO
            else config.password
        )
        forgetPasswordButton.visibility = if (config.hasPassword()) VISIBLE else GONE
    }

    private fun checkForUrlCredentials() {
        val text = configUrlView.editText!!.text.toString()
        Uri.parse(text)?.userInfo?.let { userInfo ->
            if (userInfo.contains(':')) {
                val (user, pass) = userInfo.split(':')
                val strippedUrl = text.replace("${userInfo}@", "")
                configUrlView.editText!!.setText(strippedUrl)
                usernameView.editText!!.setText(user)
                passwordView.editText!!.setText(pass)
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
        progressBar.visibility = INVISIBLE
        okButton.visibility = VISIBLE
    }

}
