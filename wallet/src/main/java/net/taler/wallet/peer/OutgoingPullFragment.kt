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

package net.taler.wallet.peer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.exchanges.ExchangeItem

class OutgoingPullFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val exchangeManager get() = model.exchangeManager
    private val peerManager get() = model.peerManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val amount = arguments?.getString("amount")?.let {
            Amount.fromJSONString(it)
        } ?: error("no amount passed")
        val exchangeFlow = exchangeManager.findExchangeForCurrency(amount.currency)
        return ComposeView(requireContext()).apply {
            setContent {
                MdcTheme {
                    Surface {
                        val state = peerManager.pullState.collectAsStateLifecycleAware()
                        if (state.value is OutgoingIntro) {
                            val exchangeState =
                                exchangeFlow.collectAsStateLifecycleAware(initial = null)
                            OutgoingPullIntroComposable(
                                amount = amount,
                                exchangeState = exchangeState,
                                onCreateInvoice = this@OutgoingPullFragment::onCreateInvoice,
                            )
                        } else {
                            OutgoingPullResultComposable(state.value) {
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.receive_peer_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) peerManager.resetPullPayment()
    }

    private fun onCreateInvoice(amount: Amount, exchange: ExchangeItem) {
        peerManager.initiatePullPayment(amount, exchange)
    }
}
