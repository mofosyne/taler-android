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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.composethemeadapter.MdcTheme
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.collectAsStateLifecycleAware

class PullPaymentFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val peerManager get() = model.peerManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        lifecycleScope.launchWhenResumed {
            peerManager.paymentState.collect {
                if (it is PeerIncomingAccepted) {
                    findNavController().navigate(R.id.action_promptPullPayment_to_nav_main)
                }
            }
        }
        return ComposeView(requireContext()).apply {
            setContent {
                MdcTheme {
                    Surface {
                        val state = peerManager.paymentState.collectAsStateLifecycleAware()
                        PeerPullPaymentComposable(state) { terms ->
                            peerManager.acceptPeerPullPayment(terms)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.pay_peer_title)
    }
}
