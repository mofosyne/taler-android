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

object ByteArrayUtils {

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
