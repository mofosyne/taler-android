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

package net.taler.wallet.withdraw

import net.taler.common.Bech32.Companion.generateFakeSegwitAddress
import org.junit.Assert
import org.junit.Test

class WithdrawManagerKtTest {

    @Test
    fun generateMainnet() {
        val (addr1, addr2) = generateFakeSegwitAddress("54ZN9AMVN1R0YZ68ZPVHHQA4KZE1V037M05FNMYH4JQ596YAKJEG",
            "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")

        Assert.assertEquals(addr1, "bc1q9yl4f23f8a224xagwq8hej8akuvd63yl8nyedj")
        Assert.assertEquals(addr2, "bc1q4yl4f2kurkqx0gq2ltfazf9w2jdu48yaqlghnp")
    }

    @Test
    fun generateTestnet() {
        val (addr1, addr2) = generateFakeSegwitAddress("54ZN9AMVN1R0YZ68ZPVHHQA4KZE1V037M05FNMYH4JQ596YAKJEG",
            "tb1qhxrhccqexg0dv4nltgkuw4fg2ce7muplmjsn0v")

        Assert.assertEquals(addr1, "tb1q9yl4f23f8a224xagwq8hej8akuvd63yld4l2kp")
        Assert.assertEquals(addr2, "tb1q4yl4f2kurkqx0gq2ltfazf9w2jdu48ya2enygj")
    }

    @Test
    fun generateRegnet() {
        val (addr1, addr2) = generateFakeSegwitAddress("54ZN9AMVN1R0YZ68ZPVHHQA4KZE1V037M05FNMYH4JQ596YAKJEG",
            "bcrtqhxrhccqexg0dv4nltgkuw4fg2ce7muplmjsn0v")

        Assert.assertEquals(addr1, "bcrt1q9yl4f23f8a224xagwq8hej8akuvd63yl0ux8pg")
        Assert.assertEquals(addr2, "bcrt1q4yl4f2kurkqx0gq2ltfazf9w2jdu48yags2flm")
    }
}
