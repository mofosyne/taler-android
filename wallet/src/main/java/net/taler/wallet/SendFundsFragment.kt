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

package net.taler.wallet

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
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.peer.PeerPaymentIntro
import net.taler.wallet.peer.PeerPushIntroComposable
import net.taler.wallet.peer.PeerPushResultComposable

class SendFundsFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val transactionManager get() = model.transactionManager
    private val peerManager get() = model.peerManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent {
            MdcTheme {
                Surface {
                    val state = peerManager.pushState.collectAsStateLifecycleAware()
                    if (state.value is PeerPaymentIntro) {
                        val currency = transactionManager.selectedCurrency
                            ?: error("No currency selected")
                        PeerPushIntroComposable(currency, this@SendFundsFragment::onSend)
                    } else {
                        PeerPushResultComposable(state.value) {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.transactions_send_funds)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) peerManager.resetPushPayment()
    }

    private fun onSend(amount: Amount, summary: String) {
        peerManager.initiatePeerPushPayment(amount, summary)
    }
}
