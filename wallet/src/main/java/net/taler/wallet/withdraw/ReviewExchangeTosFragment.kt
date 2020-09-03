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

package net.taler.wallet.withdraw

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.noties.markwon.Markwon
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.MainViewModel
import net.taler.wallet.R
import net.taler.wallet.databinding.FragmentReviewExchangeTosBinding
import java.text.ParseException

class ReviewExchangeTosFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    private lateinit var ui: FragmentReviewExchangeTosBinding
    private val markwon by lazy { Markwon.builder(requireContext()).build() }
    private val adapter by lazy { TosAdapter(markwon) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        ui = FragmentReviewExchangeTosBinding.inflate(inflater, container, false)
        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ui.acceptTosCheckBox.isChecked = false
        ui.acceptTosCheckBox.setOnCheckedChangeListener { _, _ ->
            withdrawManager.acceptCurrentTermsOfService()
        }
        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                is WithdrawStatus.TosReviewRequired -> {
                    val sections = try {
                        // TODO remove next line once exchange delivers proper markdown
                        val text = it.tosText.replace("****************", "================")
                        parseTos(markwon, text)
                    } catch (e: ParseException) {
                        onTosError(e.message ?: "Unknown Error")
                        return@Observer
                    }
                    adapter.setSections(sections)
                    ui.tosList.adapter = adapter
                    ui.tosList.fadeIn()

                    ui.acceptTosCheckBox.fadeIn()
                    ui.progressBar.fadeOut()
                }
                is WithdrawStatus.Loading -> {
                    findNavController().navigate(R.id.action_reviewExchangeTOS_to_promptWithdraw)
                }
                is WithdrawStatus.ReceivedDetails -> {
                    findNavController().navigate(R.id.action_reviewExchangeTOS_to_promptWithdraw)
                }
            }
        })
    }

    private fun onTosError(msg: String) {
        ui.tosList.fadeIn()
        ui.progressBar.fadeOut()
        ui.buttonCard.fadeOut()
        ui.errorView.text = getString(R.string.exchange_tos_error, "\n\n$msg")
        ui.errorView.fadeIn()
    }

}
