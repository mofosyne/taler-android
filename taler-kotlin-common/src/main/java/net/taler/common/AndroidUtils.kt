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

package net.taler.common

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View

fun View.fadeIn(endAction: () -> Unit = {}) {
    if (visibility == View.VISIBLE) return
    alpha = 0f
    visibility = View.VISIBLE
    animate().alpha(1f).withEndAction {
        endAction.invoke()
    }.start()
}

fun View.fadeOut(endAction: () -> Unit = {}) {
    if (visibility == View.INVISIBLE) return
    animate().alpha(0f).withEndAction {
        visibility = View.INVISIBLE
        alpha = 1f
        endAction.invoke()
    }.start()
}

fun Context.isOnline(): Boolean {
    val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (Build.VERSION.SDK_INT < 29) {
        @Suppress("DEPRECATION")
        cm.activeNetworkInfo?.isConnected == true
    } else {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
