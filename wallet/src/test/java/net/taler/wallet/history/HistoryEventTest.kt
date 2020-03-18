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
import net.taler.wallet.history.RefreshReason.PAY
import net.taler.wallet.history.ReserveType.MANUAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HistoryEventTest {

    private val mapper = ObjectMapper().registerModule(KotlinModule())

    private val timestamp = Random.nextLong()
    private val exchangeBaseUrl = "https://exchange.test.taler.net/"
    private val orderShortInfo = OrderShortInfo(
        proposalId = "EP5MH4R5C9RMNA06YS1QGEJ3EY682PY8R1SGRFRP74EV735N3ATG",
        orderId = "2019.364-01RAQ68DQ7AWR",
        merchantBaseUrl = "https://backend.demo.taler.net/public/instances/FSF/",
        amount = "KUDOS:0.5",
        summary = "Essay: Foreword"
    )

    @Test
    fun `test ExchangeAddedEvent`() {
        val builtIn = Random.nextBoolean()
        val json = """{
            "type": "exchange-added",
            "builtIn": $builtIn,
            "eventId": "exchange-added;https%3A%2F%2Fexchange.test.taler.net%2F",
            "exchangeBaseUrl": "https:\/\/exchange.test.taler.net\/",
            "timestamp": {
                "t_ms": $timestamp
            }
        }""".trimIndent()
        val event: ExchangeAddedEvent = mapper.readValue(json)

        assertEquals(builtIn, event.builtIn)
        assertEquals(exchangeBaseUrl, event.exchangeBaseUrl)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test ExchangeUpdatedEvent`() {
        val json = """{
            "type": "exchange-updated",
            "eventId": "exchange-updated;https%3A%2F%2Fexchange.test.taler.net%2F",
            "exchangeBaseUrl": "https:\/\/exchange.test.taler.net\/",
            "timestamp": {
                "t_ms": $timestamp
            }
        }""".trimIndent()
        val event: ExchangeUpdatedEvent = mapper.readValue(json)

        assertEquals(exchangeBaseUrl, event.exchangeBaseUrl)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test ReserveShortInfo`() {
        val json = """{
            "exchangeBaseUrl": "https:\/\/exchange.test.taler.net\/",
            "reserveCreationDetail": {
                "type": "manual"
            },
            "reservePub": "BRT2P0YMQSD5F48V9XHVNH73ZTS6EZC0KCQCPGPZQWTSQB77615G"
        }""".trimIndent()
        val info: ReserveShortInfo = mapper.readValue(json)

        assertEquals(exchangeBaseUrl, info.exchangeBaseUrl)
        assertEquals(MANUAL, info.reserveCreationDetail.type)
        assertEquals("BRT2P0YMQSD5F48V9XHVNH73ZTS6EZC0KCQCPGPZQWTSQB77615G", info.reservePub)
    }

    @Test
    fun `test ReserveBalanceUpdatedEvent`() {
        val json = """{
            "type": "reserve-balance-updated",
            "eventId": "reserve-balance-updated;K0H10Q6HB9WH0CKHQQMNH5C6GA7A9AR1E2XSS9G1KG3ZXMBVT26G",
            "amountExpected": "TESTKUDOS:23",
            "amountReserveBalance": "TESTKUDOS:10",
            "timestamp": {
                "t_ms": $timestamp
            },
            "newHistoryTransactions": [
                {
                    "amount": "TESTKUDOS:10",
                    "sender_account_url": "payto:\/\/x-taler-bank\/bank.test.taler.net\/894",
                    "timestamp": {
                        "t_ms": $timestamp
                    },
                    "wire_reference": "00000000004TR",
                    "type": "DEPOSIT"
                }
            ],
            "reserveShortInfo": {
                "exchangeBaseUrl": "https:\/\/exchange.test.taler.net\/",
                "reserveCreationDetail": {
                    "type": "manual"
                },
                "reservePub": "BRT2P0YMQSD5F48V9XHVNH73ZTS6EZC0KCQCPGPZQWTSQB77615G"
            }
        }""".trimIndent()
        val event: ReserveBalanceUpdatedEvent = mapper.readValue(json)

        assertEquals(timestamp, event.timestamp.ms)
        assertEquals("TESTKUDOS:23", event.amountExpected)
        assertEquals("TESTKUDOS:10", event.amountReserveBalance)
        assertEquals(1, event.newHistoryTransactions.size)
        assertTrue(event.newHistoryTransactions[0] is ReserveDepositTransaction)
        assertEquals(exchangeBaseUrl, event.reserveShortInfo.exchangeBaseUrl)
    }

    @Test
    fun `test HistoryWithdrawnEvent`() {
        val json = """{
            "type": "withdrawn",
            "withdrawSessionId": "974FT7JDNR20EQKNR21G1HV9PB6T5AZHYHX9NHR51Q30ZK3T10S0",
            "eventId": "withdrawn;974FT7JDNR20EQKNR21G1HV9PB6T5AZHYHX9NHR51Q30ZK3T10S0",
            "amountWithdrawnEffective": "TESTKUDOS:9.8",
            "amountWithdrawnRaw": "TESTKUDOS:10",
            "exchangeBaseUrl": "https:\/\/exchange.test.taler.net\/",
            "timestamp": {
                "t_ms": $timestamp
            },
            "withdrawalSource": {
                "type": "reserve",
                "reservePub": "BRT2P0YMQSD5F48V9XHVNH73ZTS6EZC0KCQCPGPZQWTSQB77615G"
            }
        }""".trimIndent()
        val event: HistoryWithdrawnEvent = mapper.readValue(json)

        assertEquals(
            "974FT7JDNR20EQKNR21G1HV9PB6T5AZHYHX9NHR51Q30ZK3T10S0",
            event.withdrawSessionId
        )
        assertEquals("TESTKUDOS:9.8", event.amountWithdrawnEffective)
        assertEquals("TESTKUDOS:10", event.amountWithdrawnRaw)
        assertTrue(event.withdrawalSource is WithdrawalSourceReserve)
        assertEquals(
            "BRT2P0YMQSD5F48V9XHVNH73ZTS6EZC0KCQCPGPZQWTSQB77615G",
            (event.withdrawalSource as WithdrawalSourceReserve).reservePub
        )
        assertEquals(exchangeBaseUrl, event.exchangeBaseUrl)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test OrderShortInfo`() {
        val json = """{
            "amount": "KUDOS:0.5",
            "orderId": "2019.364-01RAQ68DQ7AWR",
            "merchantBaseUrl": "https:\/\/backend.demo.taler.net\/public\/instances\/FSF\/",
            "proposalId": "EP5MH4R5C9RMNA06YS1QGEJ3EY682PY8R1SGRFRP74EV735N3ATG",
            "summary": "Essay: Foreword"
        }""".trimIndent()
        val info: OrderShortInfo = mapper.readValue(json)

        assertEquals("KUDOS:0.5", info.amount)
        assertEquals("2019.364-01RAQ68DQ7AWR", info.orderId)
        assertEquals("Essay: Foreword", info.summary)
    }

    @Test
    fun `test HistoryOrderAcceptedEvent`() {
        val json = """{
            "type": "order-accepted",
            "eventId": "order-accepted;EP5MH4R5C9RMNA06YS1QGEJ3EY682PY8R1SGRFRP74EV735N3ATG",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "timestamp": {
                "t_ms": $timestamp
            }
        }""".trimIndent()
        val event: HistoryOrderAcceptedEvent = mapper.readValue(json)

        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryOrderRefusedEvent`() {
        val json = """{
            "type": "order-refused",
            "eventId": "order-refused;9RJGAYXKWX0Y3V37H66606SXSA7V2CV255EBFS4G1JSH6W1EG7F0",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "timestamp": {
                "t_ms": $timestamp
            }
        }""".trimIndent()
        val event: HistoryOrderRefusedEvent = mapper.readValue(json)

        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryPaymentSentEvent`() {
        val json = """{
            "type": "payment-sent",
            "eventId": "payment-sent;EP5MH4R5C9RMNA06YS1QGEJ3EY682PY8R1SGRFRP74EV735N3ATG",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "replay": false,
            "sessionId": "e4f436c4-3c5c-4aee-81d2-26e425c09520",
            "timestamp": {
                "t_ms": $timestamp
            },
            "numCoins": 6,
            "amountPaidWithFees": "KUDOS:0.6"
        }""".trimIndent()
        val event: HistoryPaymentSentEvent = mapper.readValue(json)

        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals(false, event.replay)
        assertEquals(6, event.numCoins)
        assertEquals("KUDOS:0.6", event.amountPaidWithFees)
        assertEquals("e4f436c4-3c5c-4aee-81d2-26e425c09520", event.sessionId)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryPaymentSentEvent without sessionId`() {
        val json = """{
            "type": "payment-sent",
            "eventId": "payment-sent;EP5MH4R5C9RMNA06YS1QGEJ3EY682PY8R1SGRFRP74EV735N3ATG",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "replay": true,
            "timestamp": {
                "t_ms": $timestamp
            },
            "numCoins": 6,
            "amountPaidWithFees": "KUDOS:0.6"
        }""".trimIndent()
        val event: HistoryPaymentSentEvent = mapper.readValue(json)

        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals(true, event.replay)
        assertEquals(6, event.numCoins)
        assertEquals("KUDOS:0.6", event.amountPaidWithFees)
        assertEquals(null, event.sessionId)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryPaymentAbortedEvent`() {
        val json = """{
            "type": "payment-aborted",
            "eventId": "payment-sent;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "timestamp": {
              "t_ms": $timestamp
            },
            "amountLost": "KUDOS:0.1"
          }""".trimIndent()
        val event: HistoryPaymentAbortedEvent = mapper.readValue(json)

        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals("KUDOS:0.1", event.amountLost)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryTipAcceptedEvent`() {
        val json = """{
            "type": "tip-accepted",
            "timestamp": {
              "t_ms": $timestamp
            },
            "eventId": "tip-accepted;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "tipId": "tip-accepted;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "tipRaw": "KUDOS:4"
          }""".trimIndent()
        val event: HistoryTipAcceptedEvent = mapper.readValue(json)

        assertEquals("tip-accepted;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0", event.tipId)
        assertEquals("KUDOS:4", event.tipRaw)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryTipDeclinedEvent`() {
        val json = """{
            "type": "tip-declined",
            "timestamp": {
              "t_ms": $timestamp
            },
            "eventId": "tip-accepted;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "tipId": "tip-accepted;998724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "tipAmount": "KUDOS:4"
          }""".trimIndent()
        val event: HistoryTipDeclinedEvent = mapper.readValue(json)

        assertEquals("tip-accepted;998724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0", event.tipId)
        assertEquals("KUDOS:4", event.tipAmount)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryRefundedEvent`() {
        val json = """{
            "type": "refund",
            "eventId": "refund;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
            "refundGroupId": "refund;998724",
            "orderShortInfo": {
                "amount": "${orderShortInfo.amount}",
                "orderId": "${orderShortInfo.orderId}",
                "merchantBaseUrl": "${orderShortInfo.merchantBaseUrl}",
                "proposalId": "${orderShortInfo.proposalId}",
                "summary": "${orderShortInfo.summary}"
            },
            "timestamp": {
              "t_ms": $timestamp
            },
            "amountRefundedRaw": "KUDOS:1.0",
            "amountRefundedInvalid": "KUDOS:0.5",
            "amountRefundedEffective": "KUDOS:0.4"
          }""".trimIndent()
        val event: HistoryRefundedEvent = mapper.readValue(json)

        assertEquals("refund;998724", event.refundGroupId)
        assertEquals("KUDOS:1.0", event.amountRefundedRaw)
        assertEquals("KUDOS:0.5", event.amountRefundedInvalid)
        assertEquals("KUDOS:0.4", event.amountRefundedEffective)
        assertEquals(orderShortInfo, event.orderShortInfo)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryRefreshedEvent`() {
        val json = """{
            "type": "refreshed",
            "refreshGroupId": "8AVHKJFAN4QV4C11P56NEY83AJMGFF2KF412AN3Y0QBP09RSN640",
            "eventId": "refreshed;8AVHKJFAN4QV4C11P56NEY83AJMGFF2KF412AN3Y0QBP09RSN640",
            "timestamp": {
                "t_ms": $timestamp
            },
            "refreshReason": "pay",
            "amountRefreshedEffective": "KUDOS:0",
            "amountRefreshedRaw": "KUDOS:1",
            "numInputCoins": 6,
            "numOutputCoins": 0,
            "numRefreshedInputCoins": 1
        }""".trimIndent()
        val event: HistoryRefreshedEvent = mapper.readValue(json)

        assertEquals("KUDOS:0", event.amountRefreshedEffective)
        assertEquals("KUDOS:1", event.amountRefreshedRaw)
        assertEquals(6, event.numInputCoins)
        assertEquals(0, event.numOutputCoins)
        assertEquals(1, event.numRefreshedInputCoins)
        assertEquals("8AVHKJFAN4QV4C11P56NEY83AJMGFF2KF412AN3Y0QBP09RSN640", event.refreshGroupId)
        assertEquals(PAY, event.refreshReason)
        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryOrderRedirectedEvent`() {
        val json = """{
            "type": "order-redirected",
            "eventId": "order-redirected;621J6D5SXG7M17TYA26945DYKNQZPW4600MZ1W8MADA1RRR49F8G",
            "alreadyPaidOrderShortInfo": {
              "amount": "KUDOS:0.5",
              "orderId": "2019.354-01P25CD66P8NG",
              "merchantBaseUrl": "https://backend.demo.taler.net/public/instances/FSF/",
              "proposalId": "898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0",
              "summary": "Essay: 1. The Free Software Definition"
            },
            "newOrderShortInfo": {
              "amount": "KUDOS:0.5",
              "orderId": "2019.364-01M4QH6KPMJY4",
              "merchantBaseUrl": "https://backend.demo.taler.net/public/instances/FSF/",
              "proposalId": "621J6D5SXG7M17TYA26945DYKNQZPW4600MZ1W8MADA1RRR49F8G",
              "summary": "Essay: 1. The Free Software Definition"
            },
            "timestamp": {
              "t_ms": $timestamp
            }
          }""".trimIndent()
        val event: HistoryOrderRedirectedEvent = mapper.readValue(json)

        assertEquals("898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0", event.alreadyPaidOrderShortInfo.proposalId)
        assertEquals("https://backend.demo.taler.net/public/instances/FSF/", event.alreadyPaidOrderShortInfo.merchantBaseUrl)
        assertEquals("2019.354-01P25CD66P8NG", event.alreadyPaidOrderShortInfo.orderId)
        assertEquals("KUDOS:0.5", event.alreadyPaidOrderShortInfo.amount)
        assertEquals("Essay: 1. The Free Software Definition", event.alreadyPaidOrderShortInfo.summary)

        assertEquals("621J6D5SXG7M17TYA26945DYKNQZPW4600MZ1W8MADA1RRR49F8G", event.newOrderShortInfo.proposalId)
        assertEquals("https://backend.demo.taler.net/public/instances/FSF/", event.newOrderShortInfo.merchantBaseUrl)
        assertEquals("2019.364-01M4QH6KPMJY4", event.newOrderShortInfo.orderId)
        assertEquals("KUDOS:0.5", event.newOrderShortInfo.amount)
        assertEquals("Essay: 1. The Free Software Definition", event.newOrderShortInfo.summary)

        assertEquals(timestamp, event.timestamp.ms)
    }

    @Test
    fun `test HistoryUnknownEvent`() {
        val json = """{
            "type": "does not exist",
            "timestamp": {
              "t_ms": $timestamp
            },
            "eventId": "does-not-exist;898724XGQ1GGMZB4WY3KND582NSP74FZ60BX0Y87FF81H0FJ8XD0"
          }""".trimIndent()
        val event: HistoryEvent = mapper.readValue(json)

        assertEquals(HistoryUnknownEvent::class.java, event.javaClass)
        assertEquals(timestamp, event.timestamp.ms)
    }

}
