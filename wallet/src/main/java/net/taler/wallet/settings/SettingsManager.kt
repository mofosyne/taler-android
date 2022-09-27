/*
 * This file is part of GNU Taler
 * (C) 2022 Taler Systems S.A.
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

package net.taler.wallet.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.taler.wallet.R

class SettingsManager(
    private val context: Context,
    private val scope: CoroutineScope,
) {

    fun exportLogcat(uri: Uri?) {
        if (uri == null) {
            onLogExportError()
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                    val command = arrayOf("logcat", "-d", "*:V")
                    val proc = Runtime.getRuntime().exec(command)
                    proc.inputStream.copyTo(outputStream)
                } ?: onLogExportError()
            } catch (e: Exception) {
                Log.e(SettingsManager::class.simpleName, "Error exporting log: ", e)
                onLogExportError()
                return@launch
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.settings_logcat_success, LENGTH_LONG).show()
            }
        }
    }

    private fun onLogExportError() {
        Toast.makeText(context, R.string.settings_logcat_error, LENGTH_LONG).show()
    }

}
