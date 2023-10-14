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

package net.taler.anastasis.shared

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object FileUtils {
    fun Context.resolveDocFilename(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }

    fun Context.resolveDocMimeType(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            cursor.moveToFirst()
            cursor.getString(mimeIndex)
        }

    // Source: https://stackoverflow.com/a/12223201
    fun InputStream.bufferedReadBytes(): ByteArray? = use {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(16384)
        var bytesRead: Int
        try {
            while (true) {
                bytesRead = it.read(data, 0, data.size)
                if (bytesRead == -1) break
                buffer.write(data)
            }
            buffer.flush()
            return buffer.toByteArray()
        } catch (e: IOException) {
            Log.d("FileUtils", e.stackTraceToString())
            return null
        }
    }
}