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

package net.taler.wallet.exchanges

import androidx.navigation.fragment.findNavController
import net.taler.common.fadeOut
import net.taler.wallet.withdraw.WithdrawStatus.ReceivedDetails

class SelectExchangeFragment : ExchangeListFragment() {

    private val withdrawManager by lazy { model.withdrawManager }

    override val isSelectOnly = true
    private val exchangeSelection by lazy {
        requireNotNull(withdrawManager.exchangeSelection.value?.getEvenIfConsumedAlready())
    }

    override fun onExchangeUpdate(exchanges: List<ExchangeItem>) {
        ui.progressBar.fadeOut()
        val status = withdrawManager.withdrawStatus.value
        if (status is ReceivedDetails) {
            super.onExchangeUpdate(status.possibleExchanges)
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onExchangeSelected(item: ExchangeItem) {
        val status = withdrawManager.withdrawStatus.value
        val exchanges = (status as? ReceivedDetails)?.possibleExchanges ?: emptyList()
        withdrawManager.getWithdrawalDetails(
            exchangeBaseUrl = item.exchangeBaseUrl,
            amount = exchangeSelection.amount,
            showTosImmediately = true,
            uri = exchangeSelection.talerWithdrawUri,
            possibleExchanges = exchanges,
        )
        findNavController().navigateUp()
    }

}
