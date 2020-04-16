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

package net.taler.wallet

import android.app.Activity
import com.google.zxing.integration.android.IntentIntegrator

fun scanQrCode(activity: Activity) {
    IntentIntegrator(activity).apply {
        setPrompt("")
        setBeepEnabled(true)
        setOrientationLocked(false)
    }.initiateScan(listOf(IntentIntegrator.QR_CODE))
}

fun cleanExchange(exchange: String) = exchange.let {
    if (it.startsWith("https://")) it.substring(8) else it
}.trimEnd('/')
