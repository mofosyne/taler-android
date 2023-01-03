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

// Copyright (c) 2020 Figure Technologies Inc.
// The contents of this file were derived from an implementation
// by the btcsuite developers https://github.com/btcsuite/btcutil.

// Copyright (c) 2017 The btcsuite developers
// Use of this source code is governed by an ISC
// license that can be found in the LICENSE file.

// modified version of https://gist.github.com/iramiller/4ebfcdfbc332a9722c4a4abeb4e16454

package net.taler.common

import java.util.Locale.ROOT
import kotlin.experimental.and
import kotlin.experimental.or

infix fun Int.min(b: Int): Int = b.takeIf { this > b } ?: this
infix fun UByte.shl(bitCount: Int) = ((this.toInt() shl bitCount) and 0xff).toUByte()
infix fun UByte.shr(bitCount: Int) = (this.toInt() shr bitCount).toUByte()

/**
 * Bech32 Data encoding instance containing data for encoding as well as a human readable prefix
 */
data class Bech32Data(val hrp: String, val fiveBitData: ByteArray) {

    /**
     * The encapsulated data as typical 8bit bytes.
     */
    val data = Bech32.convertBits(fiveBitData, 5, 8, false)

    /**
     * Address is the Bech32 encoded value of the data prefixed with the human readable portion and
     * protected by an appended checksum.
     */
    val address = Bech32.encode(hrp, fiveBitData)

    /**
     * Checksum for encapsulated data + hrp
     */
    val checksum = Bech32.checksum(this.hrp, this.fiveBitData.toTypedArray())

    /**
     * The Bech32 Address toString prints state information for debugging purposes.
     * @see address() for the bech32 encoded address string output.
     */
    override fun toString(): String {
        return "bech32 : ${this.address}\nhuman: ${this.hrp} \nbytes"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bech32Data

        if (hrp != other.hrp) return false
        if (!fiveBitData.contentEquals(other.fiveBitData)) return false
        if (!data.contentEquals(other.data)) return false
        if (address != other.address) return false
        if (!checksum.contentEquals(other.checksum)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hrp.hashCode()
        result = 31 * result + fiveBitData.contentHashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + address.hashCode()
        result = 31 * result + checksum.contentHashCode()
        return result
    }
}

/**
 * BIP173 compliant processing functions for handling Bech32 encoding for addresses
 */
class Bech32 {

    companion object {
        const val CHECKSUM_SIZE = 6
        private const val MIN_VALID_LENGTH = 8
        private const val MAX_VALID_LENGTH = 90
        const val MIN_VALID_CODEPOINT = 33
        private const val MAX_VALID_CODEPOINT = 126

        const val charset = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
        private val gen = intArrayOf(0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3)

        fun generateFakeSegwitAddress(reservePub: String?, addr: String): List<String> {
            if (reservePub == null || reservePub.isEmpty()) return listOf()
            val pub = CyptoUtils.decodeCrock(reservePub)
            if (pub.size != 32) return listOf()

            val firstRnd = pub.copyOfRange(0, 4)
            val secondRnd = pub.copyOfRange(0, 4)

            firstRnd[0] = firstRnd[0].and(0b0111_1111)
            secondRnd[0] = secondRnd[0].or(0b1000_0000.toByte())

            val firstPart = ByteArray(20)
            firstRnd.copyInto(firstPart, 0, 0, 4)
            pub.copyInto(firstPart, 4, 0, 16)

            val secondPart = ByteArray(20)
            secondRnd.copyInto(secondPart, 0, 0, 4)
            pub.copyInto(secondPart, 4, 16, 32)

            val zero = ByteArray(1)
            zero[0] = 0
            val hrp = when {
                addr[0] == 'b' && addr[1] == 'c' && addr[2] == 'r' && addr[3] == 't' -> "bcrt"
                addr[0] == 't' && addr[1] == 'b' -> "tb"
                addr[0] == 'b' && addr[1] == 'c' -> "bc"
                else -> throw Error("unknown bitcoin net")
            }

            return listOf(
                Bech32Data(hrp, zero + convertBits(firstPart, 8, 5, true)).address,
                Bech32Data(hrp, zero + convertBits(secondPart, 8, 5, true)).address,
            )
        }

        /**
         * Decodes a Bech32 String
         */
        fun decode(bech32: String): Bech32Data {
            require(bech32.length in MIN_VALID_LENGTH..MAX_VALID_LENGTH) { "invalid bech32 string length" }
            require(bech32.toCharArray()
                .none { c -> c.code < MIN_VALID_CODEPOINT || c.code > MAX_VALID_CODEPOINT })
            {
                "invalid character in bech32: ${
                    bech32.toCharArray().map { c -> c.code }
                        .filter { c -> c < MIN_VALID_CODEPOINT || c > MAX_VALID_CODEPOINT }
                }"
            }

            require(bech32 == bech32.lowercase(ROOT) || bech32 == bech32.uppercase(ROOT))
            { "bech32 must be either all upper or lower case" }
            require(bech32.substring(1).dropLast(CHECKSUM_SIZE)
                .contains('1')) { "invalid index of '1'" }

            val hrp = bech32.substringBeforeLast('1').lowercase(ROOT)
            val dataString = bech32.substringAfterLast('1').lowercase(ROOT)

            require(dataString.toCharArray()
                .all { c -> charset.contains(c) }) { "invalid data encoding character in bech32" }

            val dataBytes = dataString.map { c -> charset.indexOf(c).toByte() }.toByteArray()
            val checkBytes =
                dataString.takeLast(CHECKSUM_SIZE).map { c -> charset.indexOf(c).toByte() }
                    .toByteArray()

            val actualSum = checksum(hrp, dataBytes.dropLast(CHECKSUM_SIZE).toTypedArray())
            require(1 == polymod(expandHrp(hrp).plus(dataBytes.map { d -> d.toInt() }))) { "checksum failed: $checkBytes != $actualSum" }

            return Bech32Data(hrp, dataBytes.dropLast(CHECKSUM_SIZE).toByteArray())
        }

        /**
         * ConvertBits regroups bytes with toBits set based on reading groups of bits as a continuous stream group by fromBits.
         * This process is used to convert from base64 (from 8) to base32 (to 5) or the inverse.
         */
        fun convertBits(data: ByteArray, fromBits: Int, toBits: Int, pad: Boolean): ByteArray {
            require(fromBits in 1..8 && toBits in 1..8) { "only bit groups between 1 and 8 are supported" }

            // resulting bytes with each containing the toBits bits from the input set.
            val regrouped = arrayListOf<Byte>()

            var nextByte = 0.toUByte()
            var filledBits = 0

            data.forEach { d ->
                // discard unused bits.
                var b = (d.toUByte() shl (8 - fromBits))

                // How many bits remain to extract from input data.
                var remainFromBits = fromBits

                while (remainFromBits > 0) {
                    // How many bits remain to be copied in
                    val remainToBits = toBits - filledBits

                    // we extract the remaining bits unless that is more than we need.
                    val toExtract =
                        remainFromBits.takeUnless { remainToBits < remainFromBits } ?: remainToBits
                    check(toExtract >= 0) { "extract should be positive" }

                    // move existing bits to the left to make room for bits toExtract, copy in bits to extract
                    nextByte = (nextByte shl toExtract) or (b shr (8 - toExtract))

                    // discard extracted bits and update position counters
                    b = b shl toExtract
                    remainFromBits -= toExtract
                    filledBits += toExtract

                    // if we have a complete group then reset.
                    if (filledBits == toBits) {
                        regrouped.add(nextByte.toByte())
                        filledBits = 0
                        nextByte = 0.toUByte()
                    }
                }
            }

            // pad any unfinished groups as required
            if (pad && filledBits > 0) {
                nextByte = nextByte shl (toBits - filledBits)
                regrouped.add(nextByte.toByte())
                filledBits = 0
                nextByte = 0.toUByte()
            }

            return regrouped.toByteArray()
        }

        /**
         * Encodes data 5-bit bytes (data) with a given human readable portion (hrp) into a bech32 string.
         * @see convertBits for conversion or ideally use the Bech32Data extension functions
         */
        fun encode(hrp: String, fiveBitData: ByteArray): String {
            return (fiveBitData.plus(checksum(hrp, fiveBitData.toTypedArray()))
                .map { b -> charset[b.toInt()] }).joinToString("", hrp + "1")
        }

        /**
         * Calculates a bech32 checksum based on BIP 173 specification
         */
        fun checksum(hrp: String, data: Array<Byte>): ByteArray {
            val values = expandHrp(hrp)
                .plus(data.map { d -> d.toInt() })
                .plus(Array(6) { 0 }.toIntArray())

            val poly = polymod(values) xor 1

            return (0..5).map {
                ((poly shr (5 * (5 - it))) and 31).toByte()
            }.toByteArray()
        }

        /**
         * Expands the human readable prefix per BIP173 for Checksum encoding
         */
        private fun expandHrp(hrp: String) =
            hrp.map { c -> c.code shr 5 }
                .plus(0)
                .plus(hrp.map { c -> c.code and 31 })
                .toIntArray()

        /**
         * Polynomial division function for checksum calculation.  For details see BIP173
         */
        fun polymod(values: IntArray): Int {
            var chk = 1
            return values.map { num ->
                val b = chk shr 25
                chk = ((chk and 0x1ffffff) shl 5) xor num
                (0..4).map {
                    if (((b shr it) and 1) == 1) {
                        chk = chk xor gen[it]
                    }
                }
            }.let { chk }
        }
    }
}