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
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT
import com.google.android.material.snackbar.Snackbar
import net.taler.common.navigate
import net.taler.merchantpos.MainViewModel
import net.taler.merchantpos.config.ConfigFetcherFragmentDirections.Companion.actionConfigFetcherToMerchantSettings
import net.taler.merchantpos.config.ConfigFetcherFragmentDirections.Companion.actionConfigFetcherToOrder
import net.taler.merchantpos.databinding.FragmentConfigFetcherBinding

class ConfigFetcherFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val configManager by lazy { model.configManager }

    private lateinit var ui: FragmentConfigFetcherBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        ui = FragmentConfigFetcherBinding.inflate(inflater)
        return ui.root
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        configManager.fetchConfig(configManager.config, false)
        configManager.configUpdateResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                null -> return@observe
                is ConfigUpdateResult.Error -> onNetworkError(result.msg)
                is ConfigUpdateResult.Success -> {
                    navigate(actionConfigFetcherToOrder())
                }
            }
        }
    }

    private fun onNetworkError(msg: String) {
        Snackbar.make(requireView(), msg, LENGTH_SHORT).show()
        navigate(actionConfigFetcherToMerchantSettings())
    }

}
