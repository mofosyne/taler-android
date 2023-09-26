/*
 * This file is part of GNU Taler
 * (C) 2023 Taler Systems S.A.
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

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NetworkManager(context: Context) : ConnectivityManager.NetworkCallback() {
    private val connectivityManager: ConnectivityManager

    private val _networkStatus: MutableLiveData<Boolean>
    val networkStatus: LiveData<Boolean>

    init {
        // careful, the order below is important, should probably get simplified
        connectivityManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        _networkStatus = MutableLiveData(getCurrentStatus())
        networkStatus = _networkStatus
        connectivityManager.registerDefaultNetworkCallback(this)
    }

    @UiThread
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        _networkStatus.postValue(networkCapabilities.isOnline())
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        _networkStatus.postValue(getCurrentStatus())
    }

    private fun getCurrentStatus(): Boolean {
        return connectivityManager.activeNetwork?.let { network ->
            connectivityManager.getNetworkCapabilities(network)?.isOnline()
        } ?: false
    }

    private fun NetworkCapabilities.isOnline(): Boolean {
        return hasCapability(NET_CAPABILITY_INTERNET) && hasCapability(NET_CAPABILITY_VALIDATED)
    }
}
