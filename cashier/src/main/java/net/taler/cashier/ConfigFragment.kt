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
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_config.*
import net.taler.common.exhaustive

private const val URL_BANK_TEST = "https://bank.test.taler.net"
private const val URL_BANK_TEST_REGISTER = "$URL_BANK_TEST/accounts/register"

class ConfigFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            if (viewModel.config.bankUrl.isBlank()) {
                urlView.editText!!.setText(URL_BANK_TEST)
            } else {
                urlView.editText!!.setText(viewModel.config.bankUrl)
            }
            usernameView.editText!!.setText(viewModel.config.username)
            passwordView.editText!!.setText(viewModel.config.password)
        } else {
            urlView.editText!!.setText(savedInstanceState.getCharSequence("urlView"))
            usernameView.editText!!.setText(savedInstanceState.getCharSequence("usernameView"))
            passwordView.editText!!.setText(savedInstanceState.getCharSequence("passwordView"))
        }
        saveButton.setOnClickListener {
            val config = Config(
                bankUrl = urlView.editText!!.text.toString(),
                username = usernameView.editText!!.text.toString(),
                password = passwordView.editText!!.text.toString()
            )
            if (checkConfig(config)) {
                // show progress
                saveButton.visibility = INVISIBLE
                progressBar.visibility = VISIBLE
                // kick off check and observe result
                viewModel.checkAndSaveConfig(config)
                viewModel.configResult.observe(viewLifecycleOwner, onConfigResult)
                // hide keyboard
                val inputMethodManager =
                    getSystemService(requireContext(), InputMethodManager::class.java)!!
                inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        demoView.text = HtmlCompat.fromHtml(
            getString(R.string.config_demo_hint, URL_BANK_TEST_REGISTER), FROM_HTML_MODE_LEGACY
        )
        demoView.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onStart() {
        super.onStart()
        // focus on password if it is the only missing value (like after locking)
        if (urlView.editText!!.text.isNotBlank()
            && usernameView.editText!!.text.isNotBlank()
            && passwordView.editText!!.text.isBlank()
        ) {
            passwordView.editText!!.requestFocus()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // for some reason automatic restore isn't working at the moment!?
        outState.putCharSequence("urlView", urlView.editText?.text)
        outState.putCharSequence("usernameView", usernameView.editText?.text)
        outState.putCharSequence("passwordView", passwordView.editText?.text)
    }

    private fun checkConfig(config: Config): Boolean {
        if (!config.bankUrl.startsWith("https://")) {
            urlView.error = getString(R.string.config_bank_url_error)
            urlView.requestFocus()
            return false
        }
        if (config.username.isBlank()) {
            usernameView.error = getString(R.string.config_username_error)
            usernameView.requestFocus()
            return false
        }
        urlView.isErrorEnabled = false
        return true
    }

    private val onConfigResult = Observer<ConfigResult> { result ->
        if (result == null) return@Observer
        when (result) {
            is ConfigResult.Success -> {
                val action = ConfigFragmentDirections.actionConfigFragmentToBalanceFragment()
                findNavController().navigate(action)
            }
            is ConfigResult.Error -> {
                if (result.authError) {
                    Snackbar.make(view!!, R.string.config_error_auth, LENGTH_LONG).show()
                } else {
                    val str = getString(R.string.config_error, result.msg)
                    Snackbar.make(view!!, str, LENGTH_LONG).show()
                }
            }
        }.exhaustive
        saveButton.visibility = VISIBLE
        progressBar.visibility = INVISIBLE
        viewModel.configResult.removeObservers(viewLifecycleOwner)
    }

}
