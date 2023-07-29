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

package net.taler.anastasis

import android.os.Build
import io.matthewnelson.encoding.base32.Base32
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

object Utils {
    fun formatDate(date: LocalDate): String {
        val javaDate = date.toJavaLocalDate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            return javaDate.format(formatter)
        } else {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return formatter.format(date)
        }
    }

    inline fun <reified T> Json.encodeToNativeJson(value: T): JSONObject =
        JSONObject(encodeToString(value))

    fun encodeBase32 (input: String) = input
        .toByteArray(Charset.defaultCharset())
        .encodeToString(Base32.Crockford)

    fun decodeBase32 (input: String) = input
        .decodeToByteArray(Base32.Crockford)
        .toString(Charset.defaultCharset())

    // Source: https://stackoverflow.com/questions/54827455/how-to-implement-timer-with-kotlin-coroutines/54828055#54828055
    fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
        delay(initialDelay)
        while (true) {
            emit(Unit)
            delay(period)
        }
    }
}