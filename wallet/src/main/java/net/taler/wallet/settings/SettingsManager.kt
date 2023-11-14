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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.taler.wallet.R
import net.taler.wallet.backend.WalletBackendApi
import net.taler.wallet.backend.WalletResponse.Error
import net.taler.wallet.backend.WalletResponse.Success
import org.json.JSONObject

class SettingsManager(
    private val context: Context,
    private val api: WalletBackendApi,
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

    fun exportDb(uri: Uri?) {
        if (uri == null) {
            onDbExportError()
            return
        }

        scope.launch(Dispatchers.IO) {
            when (val response = api.rawRequest("exportDb")) {
                is Success -> {
                    try {
                        context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                            val data = Json.encodeToString(response.result)
                            val writer = outputStream.bufferedWriter()
                            writer.write(data)
                            writer.close()
                        }
                    } catch(e: Exception) {
                        Log.e(SettingsManager::class.simpleName, "Error exporting db: ", e)
                        withContext(Dispatchers.Main) {
                            onDbExportError()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.settings_db_export_success, LENGTH_LONG).show()
                    }
                }
                is Error -> {
                    Log.e(SettingsManager::class.simpleName, "Error exporting db: ${response.error}")
                    withContext(Dispatchers.Main) {
                        onDbExportError()
                    }
                    return@launch
                }
            }
        }
    }

    fun importDb(uri: Uri?) {
        if (uri == null) {
            onDbImportError()
            return
        }

        scope.launch(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use {  inputStream ->
                try {
                    val reader = inputStream.bufferedReader()
                    val strData = reader.readText()
                    reader.close()
                    val jsonData = JSONObject(strData)
                    when (val response = api.rawRequest("importDb") {
                        put("dump", jsonData)
                    }) {
                        is Success -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, R.string.settings_db_import_success, LENGTH_LONG).show()
                            }
                        }
                        is Error -> {
                            Log.e(SettingsManager::class.simpleName, "Error importing db: ${response.error}")
                            withContext(Dispatchers.Main) {
                                onDbImportError()
                            }
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(SettingsManager::class.simpleName, "Error importing db: ", e)
                    withContext(Dispatchers.Main) {
                        onDbImportError()
                    }
                    return@launch
                }
            }
        }
    }

    private fun onDbExportError() {
        Toast.makeText(context, R.string.settings_db_export_error, LENGTH_LONG).show()
    }

    private fun onDbImportError() {
        Toast.makeText(context, R.string.settings_db_import_error, LENGTH_LONG).show()
    }

}
