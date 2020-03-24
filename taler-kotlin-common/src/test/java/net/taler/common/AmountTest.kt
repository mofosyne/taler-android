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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AmountTest {

    @Test
    fun `test fromJSONString() works`() {
        var str = "TESTKUDOS:23.42"
        var amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("TESTKUDOS", amount.currency)
        assertEquals(23, amount.value)
        assertEquals((0.42 * 1e8).toInt(), amount.fraction)
        assertEquals("23.42 TESTKUDOS", amount.toString())

        str = "EUR:500000000.00000001"
        amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(500000000, amount.value)
        assertEquals(1, amount.fraction)
        assertEquals("500000000.00000001 EUR", amount.toString())

        str = "EUR:1500000000.00000003"
        amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(1500000000, amount.value)
        assertEquals(3, amount.fraction)
        assertEquals("1500000000.00000003 EUR", amount.toString())
    }

    @Test
    fun `test fromJSONString() accepts max values, rejects above`() {
        val maxValue = 4503599627370496
        val str = "TESTKUDOS123:$maxValue.99999999"
        val amount = Amount.fromJSONString(str)
        assertEquals(str, amount.toJSONString())
        assertEquals("TESTKUDOS123", amount.currency)
        assertEquals(maxValue, amount.value)
        assertEquals("$maxValue.99999999 TESTKUDOS123", amount.toString())

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
    fun `test JSON deserialization()`() {
        val mapper = ObjectMapper().registerModule(KotlinModule())
        var str = "TESTKUDOS:23.42"
        var amount: Amount = mapper.readValue("\"$str\"")
        assertEquals(str, amount.toJSONString())
        assertEquals("TESTKUDOS", amount.currency)
        assertEquals(23, amount.value)
        assertEquals((0.42 * 1e8).toInt(), amount.fraction)
        assertEquals("23.42 TESTKUDOS", amount.toString())

        str = "EUR:500000000.00000001"
        amount = mapper.readValue("\"$str\"")
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(500000000, amount.value)
        assertEquals(1, amount.fraction)
        assertEquals("500000000.00000001 EUR", amount.toString())

        str = "EUR:1500000000.00000003"
        amount = mapper.readValue("\"$str\"")
        assertEquals(str, amount.toJSONString())
        assertEquals("EUR", amount.currency)
        assertEquals(1500000000, amount.value)
        assertEquals(3, amount.fraction)
        assertEquals("1500000000.00000003 EUR", amount.toString())
    }

    @Test
    fun `test fromJSONString() rejections`() {
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
    fun `test fromJsonObject() works`() {
        val map = mapOf(
            "currency" to "TESTKUDOS",
            "value" to "23",
            "fraction" to "42000000"
        )

        val amount = Amount.fromJsonObject(JSONObject(map))
        assertEquals("TESTKUDOS:23.42", amount.toJSONString())
        assertEquals("TESTKUDOS", amount.currency)
        assertEquals(23, amount.value)
        assertEquals(42000000, amount.fraction)
        assertEquals("23.42 TESTKUDOS", amount.toString())
    }

    @Test
    fun `test fromJsonObject() accepts max values, rejects above`() {
        val maxValue = 4503599627370496
        val maxFraction = 99999999
        var map = mapOf(
            "currency" to "TESTKUDOS123",
            "value" to "$maxValue",
            "fraction" to "$maxFraction"
        )

        val amount = Amount.fromJsonObject(JSONObject(map))
        assertEquals("TESTKUDOS123:$maxValue.$maxFraction", amount.toJSONString())
        assertEquals("TESTKUDOS123", amount.currency)
        assertEquals(maxValue, amount.value)
        assertEquals(maxFraction, amount.fraction)
        assertEquals("$maxValue.$maxFraction TESTKUDOS123", amount.toString())

        // longer currency not accepted
        assertThrows<AmountParserException>("longer currency was accepted") {
            map = mapOf(
                "currency" to "TESTKUDOS1234",
                "value" to "$maxValue",
                "fraction" to "$maxFraction"
            )
            Amount.fromJsonObject(JSONObject(map))
        }

        // max value + 1 not accepted
        assertThrows<AmountParserException>("max value + 1 was accepted") {
            map = mapOf(
                "currency" to "TESTKUDOS123",
                "value" to "${maxValue + 1}",
                "fraction" to "$maxFraction"
            )
            Amount.fromJsonObject(JSONObject(map))
        }

        // max fraction + 1 not accepted
        assertThrows<AmountParserException>("max fraction + 1 was accepted") {
            map = mapOf(
                "currency" to "TESTKUDOS123",
                "value" to "$maxValue",
                "fraction" to "${maxFraction + 1}"
            )
            Amount.fromJsonObject(JSONObject(map))
        }
    }

    @Test
    fun `test addition`() {
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
    fun `test times`() {
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
        assertEquals(
            Amount.fromJSONString("EUR:1500000000.00000003"),
            Amount.fromJSONString("EUR:500000000.00000001") * 3
        )
        assertThrows<AmountOverflowException>("times didn't overflow") {
            Amount.fromJSONString("EUR:4000000000000000") * 2
        }
    }

    @Test
    fun `test subtraction`() {
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
    fun `test isZero()`() {
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
    fun `test comparision`() {
        assertTrue(Amount.fromJSONString("EUR:0") <= Amount.fromJSONString("EUR:0"))
        assertTrue(Amount.fromJSONString("EUR:0") <= Amount.fromJSONString("EUR:0.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0") < Amount.fromJSONString("EUR:0.00000001"))
        assertTrue(Amount.fromJSONString("EUR:0") < Amount.fromJSONString("EUR:1"))
        assertTrue(Amount.fromJSONString("EUR:0") == Amount.fromJSONString("EUR:0"))
        assertTrue(Amount.fromJSONString("EUR:42") == Amount.fromJSONString("EUR:42"))
        assertTrue(Amount.fromJSONString("EUR:42.00000001") == Amount.fromJSONString("EUR:42.00000001"))
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

    private inline fun <reified T : Throwable> assertThrows(
        msg: String? = null,
        function: () -> Any
    ) {
        try {
            function.invoke()
            fail(msg)
        } catch (e: Exception) {
            assertTrue(e is T)
        }
    }

}
