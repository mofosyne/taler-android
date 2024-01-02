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
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import net.taler.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.exchanges.ExchangeItem
import net.taler.wallet.showError

class OutgoingPullFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val peerManager get() = model.peerManager
    private val transactionManager get() = model.transactionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val amount = arguments?.getString("amount")?.let {
            Amount.fromJSONString(it)
        } ?: error("no amount passed")
        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    val state = peerManager.pullState.collectAsStateLifecycleAware().value
                    OutgoingPullComposable(
                        amount = amount,
                        state = state,
                        onCreateInvoice = this@OutgoingPullFragment::onCreateInvoice,
                        onClose = {
                            findNavController().navigate(R.id.action_nav_peer_pull_to_nav_main)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                peerManager.pullState.collect {
                    if (it is OutgoingResponse) {
                        if (transactionManager.selectTransaction(it.transactionId)) {
                            findNavController().navigate(R.id.action_nav_peer_pull_to_nav_transactions_detail_peer)
                        } else {
                            findNavController().navigate(R.id.action_nav_peer_pull_to_nav_main)
                        }
                    }

                    if (it is OutgoingError && model.devMode.value == true) {
                        showError(it.info)
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

    private fun onCreateInvoice(amount: Amount, summary: String, hours: Long, exchange: ExchangeItem) {
        peerManager.initiatePeerPullCredit(amount, summary, hours, exchange)
    }
}
