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
import kotlinx.android.synthetic.main.fragment_review_exchange_tos.*
import net.taler.common.fadeIn
import net.taler.common.fadeOut
import net.taler.wallet.R
import net.taler.wallet.MainViewModel

class ReviewExchangeTosFragment : Fragment() {

    private val model: MainViewModel by activityViewModels()
    private val withdrawManager by lazy { model.withdrawManager }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_review_exchange_tos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        acceptTosCheckBox.isChecked = false
        acceptTosCheckBox.setOnCheckedChangeListener { _, _ ->
            withdrawManager.acceptCurrentTermsOfService()
        }
        withdrawManager.withdrawStatus.observe(viewLifecycleOwner, Observer {
            when (it) {
                is WithdrawStatus.TermsOfServiceReviewRequired -> {
                    tosTextView.text = it.tosText
                    tosTextView.fadeIn()
                    acceptTosCheckBox.fadeIn()
                    progressBar.fadeOut()
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

}
