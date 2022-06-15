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

package net.taler.common

import kotlin.math.floor

object CyptoUtils {
    internal fun getValue(c: Char): Int {
        val a = when (c) {
            'o','O' -> '0'
            'i','I','l','L' -> '1'
            'u','U' -> 'V'
            else -> c
        }
        if (a in '0'..'9') {
            return a - '0'
        }
        val A = if (a in 'a'..'z') a.uppercaseChar() else a
        var dec = 0
        if (A in 'A'..'Z') {
            if ('I' < A) dec++
            if ('L' < A) dec++
            if ('O' < A) dec++
            if ('U' < A) dec++
            return A - 'A' + 10 - dec
        }
        throw Error("encoding error")
    }

    fun decodeCrock(e: String): ByteArray {
        val size = e.length
        var bitpos = 0
        var bitbuf = 0
        var readPosition = 0
        val outLen = floor((size * 5f) / 8).toInt()
        val out = ByteArray(outLen)
        var outPos = 0
        while (readPosition < size || bitpos > 0) {
            if (readPosition < size) {
                val v = getValue(e[readPosition++])
                bitbuf = bitbuf.shl(5).or(v)
                bitpos += 5
            }
            while (bitpos >= 8) {
                val d = bitbuf.shr(bitpos -8).and(0xff).toByte()
                out[outPos++] = d
                bitpos -= 8
            }
            if (readPosition == size && bitpos > 0) {
                bitbuf = bitbuf.shl( 8 - bitpos).and(0xff)
                bitpos = if (bitbuf == 0) 0 else 8
            }
        }
        return out
    }

}