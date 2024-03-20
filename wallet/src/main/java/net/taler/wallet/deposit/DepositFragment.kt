/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.deposit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.serialization.json.Json
import net.taler.common.Amount
import net.taler.common.showError
import net.taler.wallet.CURRENCY_BTC
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.balances.ScopeInfo
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.showError

class DepositFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val depositManager get() = model.depositManager
    private val balanceManager get() = model.balanceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val amount = arguments?.getString("amount")?.let {
            Amount.fromJSONString(it)
        } ?: error("no amount passed")
        val scopeInfo: ScopeInfo? = arguments?.getString("scopeInfo")?.let {
            Json.decodeFromString(it)
        }
        val spec = scopeInfo?.let { balanceManager.getSpecForScopeInfo(it) }
        val receiverName = arguments?.getString("receiverName")
        val iban = arguments?.getString("IBAN")
        if (receiverName != null && iban != null) {
            onDepositButtonClicked(amount, receiverName, iban)
        }
        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    val state = depositManager.depositState.collectAsStateLifecycleAware()
                    if (amount.currency == CURRENCY_BTC) MakeBitcoinDepositComposable(
                        state = state.value,
                        amount = amount.withSpec(spec),
                        bitcoinAddress = null,
                        onMakeDeposit = { amount, bitcoinAddress ->
                            depositManager.onDepositButtonClicked(amount, bitcoinAddress)
                        },
                    ) else MakeDepositComposable(
                        state = state.value,
                        amount = amount.withSpec(spec),
                        presetName = receiverName,
                        presetIban = iban,
                        onMakeDeposit = this@DepositFragment::onDepositButtonClicked,
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenStarted {
            depositManager.depositState.collect { state ->
                if (state is DepositState.Error) {
                    if (model.devMode.value == false) {
                        showError(state.error.userFacingMsg)
                    } else {
                        showError(state.error)
                    }
                } else if (state is DepositState.Success) {
                    findNavController().navigate(R.id.action_nav_deposit_to_nav_main)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.send_deposit_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            depositManager.resetDepositState()
        }
    }

    private fun onDepositButtonClicked(
        amount: Amount,
        receiverName: String,
        iban: String,
    ) {
        depositManager.onDepositButtonClicked(amount, receiverName, iban)
    }
}
