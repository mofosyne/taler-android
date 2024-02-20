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

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.DecimalFormatSymbols
import kotlin.random.Random

class AmountTest {

    companion object {
        fun getRandomAmount() = getRandomAmount(getRandomString(1, Random.nextInt(1, 12)))
        fun getRandomAmount(currency: String): Amount {
            val value = Random.nextLong(0, Amount.MAX_VALUE)
            val fraction = Random.nextInt(0, Amount.MAX_FRACTION)
            return Amount(currency, value, fraction)
        }
    }

    @Test
    fun testFromJSONString() {
        var str = "TESTKUDOS:23.42"
        var amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("TESTKUDOS", amount.currency)
        assertEquals(23, amount.value)
        assertEquals((0.42 * 1e8).toInt(), amount.fraction)

        str = "EUR:500000000.00000001"
        amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(500000000, amount.value)
        assertEquals(1, amount.fraction)

        str = "EUR:1500000000.00000003"
        amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(1500000000, amount.value)
        assertEquals(3, amount.fraction)
    }

    @Test
    fun testToString() {
        val symbols = DecimalFormatSymbols.getInstance()
        symbols.decimalSeparator = '.'
        symbols.groupingSeparator = ','
        symbols.monetaryDecimalSeparator = '.'
        if (Build.VERSION.SDK_INT >= 34) {
            symbols.monetaryGroupingSeparator = ','
        }

        val spec = CurrencySpecification(
            name = "Bitcoin",
            numFractionalInputDigits = 8,
            numFractionalNormalDigits = 8,
            numFractionalTrailingZeroDigits = 0,
            altUnitNames = mapOf(
                0 to "₿",
                // TODO: uncomment when units get implemented
                //  and then write tests for units, please
//                -1 to "d₿",
//                -2 to "c₿",
//                -3 to "m₿",
//                -6 to "µ₿",
//                -8 to "sat",
            ),
        )

        Amount.fromString("BITCOINBTC", "0.00000001").let { amount ->
            // Only the raw amount
            assertEquals("0.00000001", amount.amountStr)

            // The amount without currency spec
            assertEquals("0.00000001 BITCOINBTC", amount.toString(symbols = symbols))
            assertEquals("0.00000001", amount.toString(symbols = symbols, showSymbol = false))
            assertEquals("-0.00000001 BITCOINBTC", amount.toString(symbols = symbols, negative = true))
            assertEquals("-0.00000001", amount.toString(symbols = symbols, showSymbol = false, negative = true))

            // The amount with currency spec
            val withSpec = amount.withSpec(spec)
            assertEquals("₿0.00000001", withSpec.toString(symbols = symbols))
            assertEquals("0.00000001", withSpec.toString(symbols = symbols, showSymbol = false))
            assertEquals("-₿0.00000001", withSpec.toString(symbols = symbols, negative = true))
            assertEquals("-0.00000001", withSpec.toString(symbols = symbols, showSymbol = false, negative = true))
        }
    }

    @Test
    fun testFromJSONStringAcceptsMaxValuesRejectsAbove() {
        val maxValue = 4503599627370496
        val str = "TESTKUDOS123:$maxValue.99999999"
        val amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("TESTKUDOS123", amount.currency)
        assertEquals(maxValue, amount.value)

        // longer currency not accepted
        assertThrows<AmountParserException>("longer currency was accepted") {
            Amount.fromJSONString("TESTKUDOS1234:$maxValue.99999999")
        }

        // max value + 1 not accepted
        assertThrows<AmountParserException>("max value + 1 was accepted") {
            Amount.fromJSONString("TESTKUDOS123:${maxValue + 1}.99999999")
        }

        // max fraction + 1 not accepted
        assertThrows<AmountParserException>("max fraction + 1 was accepted") {
            Amount.fromJSONString("TESTKUDOS123:$maxValue.999999990")
        }
    }

    @Test
    fun testFromJSONStringRejections() {
        assertThrows<AmountParserException> {
            Amount.fromJSONString("TESTKUDOS:0,5")
        }
        assertThrows<AmountParserException> {
            Amount.fromJSONString("+TESTKUDOS:0.5")
        }
        assertThrows<AmountParserException> {
            Amount.fromJSONString("0.5")
        }
        assertThrows<AmountParserException> {
            Amount.fromJSONString(":0.5")
        }
        assertThrows<AmountParserException> {
            Amount.fromJSONString("EUR::0.5")
        }
        assertThrows<AmountParserException> {
            Amount.fromJSONString("EUR:.5")
        }
    }

    @Test
    fun testAddition() {
        assertEquals(
            Amount.fromJSONString("EUR:2"),
            Amount.fromJSONString("EUR:1") + Amount.fromJSONString("EUR:1")
        )
        assertEquals(
            Amount.fromJSONString("EUR:3"),
            Amount.fromJSONString("EUR:1.5") + Amount.fromJSONString("EUR:1.5")
        )
        assertEquals(
            Amount.fromJSONString("EUR:500000000.00000002"),
            Amount.fromJSONString("EUR:500000000.00000001") + Amount.fromJSONString("EUR:0.00000001")
        )
        assertThrows<AmountOverflowException>("addition didn't overflow") {
            Amount.fromJSONString("EUR:4503599627370496.99999999") + Amount.fromJSONString("EUR:0.00000001")
        }
        assertThrows<AmountOverflowException>("addition didn't overflow") {
            Amount.fromJSONString("EUR:4000000000000000") + Amount.fromJSONString("EUR:4000000000000000")
        }
    }

    @Test
    fun testTimes() {
        assertEquals(
            Amount.fromJSONString("EUR:2"),
            Amount.fromJSONString("EUR:2") * 1
        )
        assertEquals(
            Amount.fromJSONString("EUR:2"),
            Amount.fromJSONString("EUR:1") * 2
        )
        assertEquals(
            Amount.fromJSONString("EUR:4.5"),
            Amount.fromJSONString("EUR:1.5") * 3
        )
        assertEquals(Amount.fromJSONString("EUR:0"), Amount.fromJSONString("EUR:1.11") * 0)
        assertEquals(Amount.fromJSONString("EUR:1.11"), Amount.fromJSONString("EUR:1.11") * 1)
        assertEquals(Amount.fromJSONString("EUR:2.22"), Amount.fromJSONString("EUR:1.11") * 2)
        assertEquals(Amount.fromJSONString("EUR:3.33"), Amount.fromJSONString("EUR:1.11") * 3)
        assertEquals(Amount.fromJSONString("EUR:4.44"), Amount.fromJSONString("EUR:1.11") * 4)
        assertEquals(Amount.fromJSONString("EUR:5.55"), Amount.fromJSONString("EUR:1.11") * 5)
        assertEquals(
            Amount.fromJSONString("EUR:1500000000.00000003"),
            Amount.fromJSONString("EUR:500000000.00000001") * 3
        )
        assertThrows<AmountOverflowException>("times didn't overflow") {
            Amount.fromJSONString("EUR:4000000000000000") * 2
        }
    }

    @Test
    fun testSubtraction() {
        assertEquals(
            Amount.fromJSONString("EUR:0"),
            Amount.fromJSONString("EUR:1") - Amount.fromJSONString("EUR:1")
        )
        assertEquals(
            Amount.fromJSONString("EUR:1.5"),
            Amount.fromJSONString("EUR:3") - Amount.fromJSONString("EUR:1.5")
        )
        assertEquals(
            Amount.fromJSONString("EUR:500000000.00000001"),
            Amount.fromJSONString("EUR:500000000.00000002") - Amount.fromJSONString("EUR:0.00000001")
        )
        assertThrows<AmountOverflowException>("subtraction didn't underflow") {
            Amount.fromJSONString("EUR:23.42") - Amount.fromJSONString("EUR:42.23")
        }
        assertThrows<AmountOverflowException>("subtraction didn't underflow") {
            Amount.fromJSONString("EUR:0.5") - Amount.fromJSONString("EUR:0.50000001")
        }
    }

    @Test
    fun testIsZero() {
        assertTrue(Amount.zero("EUR").isZero())
        assertTrue(Amount.fromJSONString("EUR:0").isZero())
        assertTrue(Amount.fromJSONString("EUR:0.0").isZero())
        assertTrue(Amount.fromJSONString("EUR:0.00000").isZero())
        assertTrue((Amount.fromJSONString("EUR:1.001") - Amount.fromJSONString("EUR:1.001")).isZero())

        assertFalse(Amount.fromJSONString("EUR:0.00000001").isZero())
        assertFalse(Amount.fromJSONString("EUR:1.0").isZero())
        assertFalse(Amount.fromJSONString("EUR:0001.0").isZero())
    }

    @Test
    fun testComparison() {
        assertTrue(Amount.fromJSONString("EUR:0") <= Amount.fromJSONString("EUR:0"))
        assertTrue(Amount.fromJSONString("EUR:0") <= Amount.fromJSONString("EUR:0.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0") < Amount.fromJSONString("EUR:0.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0") < Amount.fromJSONString("EUR:1"))
        assertEquals(Amount.fromJSONString("EUR:0"), Amount.fromJSONString("EUR:0"))
        assertEquals(Amount.fromJSONString("EUR:42"), Amount.fromJSONString("EUR:42"))
        assertEquals(
            Amount.fromJSONString("EUR:42.00000001"),
            Amount.fromJSONString("EUR:42.00000001")
        )
        assertTrue(Amount.fromJSONString("EUR:42.00000001") >= Amount.fromJSONString("EUR:42.00000001"))
        assertTrue(Amount.fromJSONString("EUR:42.00000002") >= Amount.fromJSONString("EUR:42.00000001"))
        assertTrue(Amount.fromJSONString("EUR:42.00000002") > Amount.fromJSONString("EUR:42.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0.00000002") > Amount.fromJSONString("EUR:0.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0.00000001") > Amount.fromJSONString("EUR:0"))
        assertTrue(Amount.fromJSONString("EUR:2") > Amount.fromJSONString("EUR:1"))

        assertThrows<IllegalStateException>("could compare amounts with different currencies") {
            Amount.fromJSONString("EUR:0.5") < Amount.fromJSONString("USD:0.50000001")
        }
    }

}
