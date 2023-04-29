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
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import net.taler.common.Amount
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.compose.TalerSurface
import net.taler.wallet.compose.collectAsStateLifecycleAware
import net.taler.wallet.showError

class OutgoingPushFragment : Fragment() {
    private val model: MainViewModel by activityViewModels()
    private val peerManager get() = model.peerManager

    // hacky way to change back action until we have navigation for compose
    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            findNavController().navigate(R.id.action_nav_peer_push_to_nav_main)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val amount = arguments?.getString("amount")?.let {
            Amount.fromJSONString(it)
        } ?: error("no amount passed")

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, backPressedCallback
        )

        return ComposeView(requireContext()).apply {
            setContent {
                TalerSurface {
                    when (val state = peerManager.pushState.collectAsStateLifecycleAware().value) {
                        is OutgoingIntro, OutgoingChecking, is OutgoingChecked -> {
                            backPressedCallback.isEnabled = false
                            OutgoingPushIntroComposable(
                                state = state,
                                amount = amount,
                                onSend = this@OutgoingPushFragment::onSend,
                            )
                        }
                        OutgoingCreating, is OutgoingResponse, is OutgoingError -> {
                            backPressedCallback.isEnabled = true
                            OutgoingPushResultComposable(state) {
                                findNavController().navigate(R.id.action_nav_peer_push_to_nav_main)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launchWhenStarted {
            peerManager.pushState.collect {
                if (it is OutgoingError && model.devMode.value == true) {
                    showError(it.info)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.setTitle(R.string.send_peer_title)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) peerManager.resetPushPayment()
    }

    private fun onSend(amount: Amount, summary: String, hours: Long) {
        peerManager.initiatePeerPushDebit(amount, summary, hours)
    }
}
