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

import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE

fun View.fadeIn(endAction: () -> Unit = {}) {
    if (visibility == VISIBLE) return
    alpha = 0f
    visibility = VISIBLE
    animate().alpha(1f).withEndAction {
        if (context != null) endAction.invoke()
    }.start()
}

fun View.fadeOut(endAction: () -> Unit = {}) {
    if (visibility == INVISIBLE) return
    animate().alpha(0f).withEndAction {
        if (context == null) return@withEndAction
        visibility = INVISIBLE
        alpha = 1f
        endAction.invoke()
    }.start()
}
