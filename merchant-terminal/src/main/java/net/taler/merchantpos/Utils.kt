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

import android.content.Context
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
import android.text.format.DateUtils.FORMAT_NO_YEAR
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.text.format.DateUtils.formatDateTime
import android.text.format.DateUtils.getRelativeTimeSpanString
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.BaseTransientBottomBar.ANIMATION_MODE_FADE
import com.google.android.material.snackbar.BaseTransientBottomBar.Duration
import com.google.android.material.snackbar.Snackbar.make

object Utils {

    private const val HEX_CHARS = "0123456789ABCDEF"

    fun hexStringToByteArray(data: String): ByteArray {
        val result = ByteArray(data.length / 2)

        for (i in data.indices step 2) {
            val firstIndex = HEX_CHARS.indexOf(data[i])
            val secondIndex = HEX_CHARS.indexOf(data[i + 1])

            val octet = firstIndex.shl(4).or(secondIndex)
            result[i.shr(1)] = octet.toByte()
        }
        return result
    }


    private val HEX_CHARS_ARRAY = HEX_CHARS.toCharArray()

    @Suppress("unused")
    fun toHex(byteArray: ByteArray): String {
        val result = StringBuffer()

        byteArray.forEach {
            val octet = it.toInt()
            val firstIndex = (octet and 0xF0).ushr(4)
            val secondIndex = octet and 0x0F
            result.append(HEX_CHARS_ARRAY[firstIndex])
            result.append(HEX_CHARS_ARRAY[secondIndex])
        }
        return result.toString()
    }

}

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

fun topSnackbar(view: View, text: CharSequence, @Duration duration: Int) {
    make(view, text, duration)
        .setAnimationMode(ANIMATION_MODE_FADE)
        .setAnchorView(R.id.navHostFragment)
        .show()
}

fun topSnackbar(view: View, @StringRes resId: Int, @Duration duration: Int) {
    topSnackbar(view, view.resources.getText(resId), duration)
}

fun NavDirections.navigate(nav: NavController) = nav.navigate(this)

fun Fragment.navigate(directions: NavDirections) = findNavController().navigate(directions)

fun Long.toRelativeTime(context: Context): CharSequence {
    val now = System.currentTimeMillis()
    return if (now - this > DAY_IN_MILLIS * 2) {
        val flags = FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH or FORMAT_NO_YEAR
        formatDateTime(context, this, flags)
    } else getRelativeTimeSpanString(this, now, MINUTE_IN_MILLIS, FORMAT_ABBREV_RELATIVE)
}

class CombinedLiveData<T, K, S>(
    source1: LiveData<T>,
    source2: LiveData<K>,
    private val combine: (data1: T?, data2: K?) -> S
) : MediatorLiveData<S>() {

    private var data1: T? = null
    private var data2: K? = null

    init {
        super.addSource(source1) { t ->
            data1 = t
            value = combine(data1, data2)
        }
        super.addSource(source2) { k ->
            data2 = k
            value = combine(data1, data2)
        }
    }

    override fun <S : Any?> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }

    override fun <T : Any?> removeSource(toRemote: LiveData<T>) {
        throw UnsupportedOperationException()
    }
}

/**
 * Use this with 'when' expressions when you need it to handle all possibilities/branches.
 */
val <T> T.exhaustive: T
    get() = this
