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

object FileUtils {
//    fun Context.createTempFileForDoc(uri: Uri): File {
//        val storageDir = createTempDirectory()
//        val filename = resolveDocFilename(uri) ?: UUID.randomUUID().toString()
//        val file = File(storageDir.pathString, filename)
//        if (file.exists()) file.delete()
//        file.createNewFile()
//
//        // Read file and copy to temp file
//        val inputStream = contentResolver.openInputStream(uri)
//        inputStream?.copyTo(file.outputStream())
//        inputStream?.close()
//
//        // TODO: not the best solution!
//        // Delete file on Java VM exit for security
//        file.deleteOnExit()
//        return file
//    }

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
}