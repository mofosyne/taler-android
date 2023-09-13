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

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

object CryptoUtils {
    private const val encTableCrock = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val encTable32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

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

    fun encodeCrock(bytes: ByteArray): String {
        var sb = ""
        val size = bytes.size
        var bitBuf = 0
        var numBits = 0
        var pos = 0
        while (pos < size || numBits > 0) {
            if (pos < size && numBits < 5) {
                val d = bytes[pos++]
                bitBuf = bitBuf.shl(8).or(d.toInt())
                numBits += 8
            }
            if (numBits < 5) {
                // zero-padding
                bitBuf = bitBuf.shl(5 - numBits)
                numBits = 5
            }
            val v = bitBuf.ushr(numBits - 5).and(31)
            sb += encTableCrock[v]
            numBits -= 5
        }
        return sb
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun encodeCrock(bytes: UByteArray): String {
        var sb = ""
        val size = bytes.size
        var bitBuf = 0
        var numBits = 0
        var pos = 0
        while (pos < size || numBits > 0) {
            if (pos < size && numBits < 5) {
                val d = bytes[pos++]
                bitBuf = bitBuf.shl(8).or(d.toInt())
                numBits += 8
            }
            if (numBits < 5) {
                // zero-padding
                bitBuf = bitBuf.shl(5 - numBits)
                numBits = 5
            }
            val v = bitBuf.ushr(numBits - 5).and(31)
            sb += encTableCrock[v]
            numBits -= 5
        }
        return sb
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

    @OptIn(ExperimentalUnsignedTypes::class)
    fun encodeBase32(bytes: UByteArray): String {
        var rpos = 0
        var bits = 0
        var vbit = 0
        val result = StringBuilder()
        while (rpos < bytes.size || vbit > 0) {
            if (rpos < bytes.size && vbit < 5) {
                bits = (bits shl 8) or bytes[rpos++].toInt()
                vbit += 8
            }
            if (vbit < 5) {
                bits = bits shl (5 - vbit)
                vbit = 5
            }
            result.append(encTable32[(bits shr (vbit - 5)) and 31])
            vbit -= 5
        }
        return result.toString()
    }

    private const val SEARCH_RANGE = 16
    private const val TIME_STEP = 30.0

    private fun String.decodeHex() = chunked(2).map { it.toUInt(16).toByte() }.toByteArray()

    @OptIn(ExperimentalUnsignedTypes::class)
    internal fun generateHmacSha1(key: ByteArray, message: ByteArray): UByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        return mac.doFinal(message).toUByteArray()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun computeTotpAndCheck(
        secretKey: UByteArray,
        digits: Int,
        code: Int,
    ): Boolean {
        val now = System.currentTimeMillis()
        val epoch = floor((now / 1000.0).roundToInt() / TIME_STEP).toInt()
        (-SEARCH_RANGE until SEARCH_RANGE).forEach {  ms ->
            val movingFactor = (epoch + ms).toString(16).padStart(16, '0')
            val hmacText = generateHmacSha1(secretKey.toByteArray(), movingFactor.decodeHex())
            val offset = hmacText[hmacText.size - 1].toInt() and 0xF
            val otp = (((hmacText[offset + 0].toInt() shl 24) +
                    (hmacText[offset + 1].toInt() shl 16) +
                    (hmacText[offset + 2].toInt() shl 8) +
                    hmacText[offset + 3].toInt()) and 0x7FFFFFFF) % 10f.pow(digits).toInt()
            if (otp == code) return true
        }
        return false
    }
}