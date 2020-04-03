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

package net.taler.wallet.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import kotlinx.android.synthetic.main.fragment_history_event.*
import net.taler.common.toAbsoluteTime
import net.taler.wallet.R
import net.taler.wallet.WalletViewModel
import net.taler.wallet.cleanExchange

class HistoryEventFragment : Fragment() {

    private val model: WalletViewModel by activityViewModels()
    private val historyManager by lazy { model.historyManager }
    private val event by lazy { requireNotNull(historyManager.selectedEvent) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (model.devMode.value == true) setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history_event, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val event = event as HistoryWithdrawnEvent

        timeView.text = event.timestamp.ms.toAbsoluteTime(requireContext())
        effectiveAmountLabel.text = getString(R.string.withdraw_total)
        effectiveAmountView.text = event.amountWithdrawnEffective.toString()
        chosenAmountLabel.text = getString(R.string.amount_chosen)
        chosenAmountView.text =
            getString(R.string.amount_positive, event.amountWithdrawnRaw.toString())
        val fee = event.amountWithdrawnRaw - event.amountWithdrawnEffective
        feeView.text = getString(R.string.amount_negative, fee.toString())
        exchangeView.text = cleanExchange(event.exchangeBaseUrl)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.history_event, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.show_json -> {
                JsonDialogFragment.new(event.json.toString(2)).show(parentFragmentManager, null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}
