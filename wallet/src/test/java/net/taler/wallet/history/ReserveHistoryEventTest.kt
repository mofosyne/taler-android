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

package net.taler.wallet.history

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import net.taler.wallet.history.ReserveDepositTransaction
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class ReserveHistoryEventTest {

    private val mapper = ObjectMapper().registerModule(KotlinModule())

    private val timestamp = Random.nextLong()

    @Test
    fun `test ExchangeAddedEvent`() {
        val senderAccountUrl = "payto://x-taler-bank/bank.test.taler.net/894"
        val json = """{
            "amount": "TESTKUDOS:10",
            "sender_account_url": "payto:\/\/x-taler-bank\/bank.test.taler.net\/894",
            "timestamp": {
                "t_ms": $timestamp
            },
            "wire_reference": "00000000004TR",
            "type": "DEPOSIT"
        }""".trimIndent()
        val transaction: ReserveDepositTransaction = mapper.readValue(json)

        assertEquals("TESTKUDOS:10", transaction.amount)
        assertEquals(senderAccountUrl, transaction.senderAccountUrl)
        assertEquals("00000000004TR", transaction.wireReference)
        assertEquals(timestamp, transaction.timestamp.ms)
    }

}
