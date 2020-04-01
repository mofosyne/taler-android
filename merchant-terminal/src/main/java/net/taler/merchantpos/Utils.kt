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

import android.util.Log
import android.view.View
import androidx.annotation.StringRes
import com.android.volley.Response
import com.android.volley.VolleyError
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.BaseTransientBottomBar.Duration
import com.google.android.material.snackbar.Snackbar.make
import net.taler.merchantpos.MainActivity.Companion.TAG

fun topSnackbar(view: View, text: CharSequence, @Duration duration: Int) {
    make(view, text, duration)
        .setAnimationMode(ANIMATION_MODE_FADE)
        .setAnchorView(R.id.navHostFragment)
        .show()
}

fun topSnackbar(view: View, @StringRes resId: Int, @Duration duration: Int) {
    topSnackbar(view, view.resources.getText(resId), duration)
}

class LogErrorListener(private val onError: (error: VolleyError) -> Any) :
    Response.ErrorListener {

    override fun onErrorResponse(error: VolleyError) {
        val body = error.networkResponse.data?.let { String(it) }
        Log.e(TAG, "$error $body")
        onError.invoke(error)
    }

}
