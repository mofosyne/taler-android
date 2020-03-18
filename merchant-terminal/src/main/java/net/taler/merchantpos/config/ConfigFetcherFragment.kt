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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.R
import net.taler.merchantpos.config.ConfigFetcherFragmentDirections.Companion.actionConfigFetcherToMerchantSettings
import net.taler.merchantpos.config.ConfigFetcherFragmentDirections.Companion.actionConfigFetcherToOrder

class ConfigFetcherFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val configManager by lazy { model.configManager }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_config_fetcher, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        configManager.fetchConfig(configManager.config, false)
        configManager.configUpdateResult.observe(viewLifecycleOwner, Observer { result ->
            when (result) {
                null -> return@Observer
                is ConfigUpdateResult.Error -> onNetworkError(result.msg)
                is ConfigUpdateResult.Success -> {
                    navigate(actionConfigFetcherToOrder())
                }
            }
        })
    }

    private fun onNetworkError(msg: String) {
        Snackbar.make(view!!, msg, LENGTH_SHORT).show()
        navigate(actionConfigFetcherToMerchantSettings())
    }

}
