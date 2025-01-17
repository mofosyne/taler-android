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

import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Looper
import android.text.format.DateUtils.DAY_IN_MILLIS
import android.text.format.DateUtils.FORMAT_ABBREV_ALL
import android.text.format.DateUtils.FORMAT_ABBREV_MONTH
import android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
import android.text.format.DateUtils.FORMAT_NO_YEAR
import android.text.format.DateUtils.FORMAT_SHOW_DATE
import android.text.format.DateUtils.FORMAT_SHOW_TIME
import android.text.format.DateUtils.FORMAT_SHOW_YEAR
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.text.format.DateUtils.formatDateTime
import android.text.format.DateUtils.getRelativeTimeSpanString
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import net.taler.lib.android.ErrorBottomSheet

fun View.fadeIn(endAction: () -> Unit = {}) {
    if (visibility == VISIBLE && alpha == 1f) return
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

fun View.hideKeyboard() {
    getSystemService(context, InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(windowToken, 0)
}

fun assertUiThread() {
    check(Looper.getMainLooper().thread == Thread.currentThread())
}

/**
 * Use this with 'when' expressions when you need it to handle all possibilities/branches.
 */
val <T> T.exhaustive: T
    get() = this

@RequiresPermission(ACCESS_NETWORK_STATE)
fun Context.isOnline(): Boolean {
    val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    return if (SDK_INT < 29) {
        @Suppress("DEPRECATION")
        cm.activeNetworkInfo?.isConnected == true
    } else {
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        capabilities.hasCapability(NET_CAPABILITY_INTERNET)
    }
}

fun FragmentActivity.showError(mainText: String, detailText: String = "") = ErrorBottomSheet
    .newInstance(mainText, detailText)
    .show(supportFragmentManager, "ERROR_BOTTOM_SHEET")

fun FragmentActivity.showError(@StringRes mainId: Int, detailText: String = "") {
    showError(getString(mainId), detailText)
}

fun Fragment.showError(mainText: String, detailText: String = "") = ErrorBottomSheet
    .newInstance(mainText, detailText)
    .show(parentFragmentManager, "ERROR_BOTTOM_SHEET")

fun Fragment.showError(@StringRes mainId: Int, detailText: String = "") {
    showError(getString(mainId), detailText)
}

fun Context.startActivitySafe(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Log.e("taler-kotlin-android", "Error starting $intent", e)
    }
}

fun Context.openUri(uri: String, title: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(uri)
    }

    startActivitySafe(Intent.createChooser(intent, title))
}

fun Context.shareText(text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }

    startActivitySafe(Intent.createChooser(intent, null))
}

fun Fragment.navigate(directions: NavDirections) = findNavController().navigate(directions)

fun Long.toRelativeTime(context: Context): CharSequence {
    val now = System.currentTimeMillis()
    return if (now - this > DAY_IN_MILLIS * 2) {
        val flags = FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_ABBREV_MONTH or FORMAT_NO_YEAR
        formatDateTime(context, this, flags)
    } else getRelativeTimeSpanString(this, now, MINUTE_IN_MILLIS, FORMAT_ABBREV_RELATIVE)
}

fun Long.toAbsoluteTime(context: Context): CharSequence {
    val flags = FORMAT_SHOW_TIME or FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR
    return formatDateTime(context, this, flags)
}

fun Long.toShortDate(context: Context): CharSequence {
    val flags = FORMAT_SHOW_DATE or FORMAT_SHOW_YEAR or FORMAT_ABBREV_ALL
    return formatDateTime(context, this, flags)
}

fun Version.getIncompatibleStringOrNull(context: Context, otherVersion: String): String? {
    val other = Version.parse(otherVersion) ?: return context.getString(R.string.version_invalid)
    val match = compare(other) ?: return context.getString(R.string.version_invalid)
    if (match.compatible) return null
    if (match.currentCmp < 0) return context.getString(R.string.version_too_old)
    if (match.currentCmp > 0) return context.getString(R.string.version_too_new)
    throw AssertionError("$this == $other")
}
