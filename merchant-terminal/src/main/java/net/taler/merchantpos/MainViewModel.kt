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

package net.taler.merchantpos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import net.taler.merchantlib.MerchantApi
import net.taler.merchantlib.getDefaultHttpClient
import net.taler.merchantpos.config.ConfigManager
import net.taler.merchantpos.history.HistoryManager
import net.taler.merchantpos.order.OrderManager
import net.taler.merchantpos.payment.PaymentManager
import net.taler.merchantpos.refund.RefundManager

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val httpClient = getDefaultHttpClient()
    private val api = MerchantApi(httpClient)

    val orderManager = OrderManager(app)
    val configManager = ConfigManager(app, viewModelScope, httpClient, api).apply {
        addConfigurationReceiver(orderManager)
    }
    val paymentManager = PaymentManager(app, configManager, viewModelScope, api)
    val historyManager = HistoryManager(configManager, viewModelScope, api)
    val refundManager = RefundManager(configManager, viewModelScope, api)

    override fun onCleared() {
        httpClient.close()
    }

}
