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

package net.taler.wallet.crypto

import org.junit.Test

class Base32CrockfordTest {
    @Test
    fun testBasic() {
        val inputStr = "Hello, World"
        val data = inputStr.toByteArray(Charsets.UTF_8)
        val enc = Base32Crockford.encode(data)
        println(enc)
        val dec = Base32Crockford.decode(enc)
        val recoveredInputStr = dec.toString(Charsets.UTF_8)
        println(recoveredInputStr)

        val foo =
            Base32Crockford.decode("51R7ARKCD5HJTTV5F4G0M818E9SP280A40G2GVH04CR30H2365338E9G6RT4AH1N6H13EGHR70RK6H1S6X2M4CSP8CSK8E1G88VKJH25610KGCHR8RWM4DJ47123CH9K89334D1S8N24ACJ48CR3EH256MR3AH1R711KCE9N6S134GSN6RW46D1H6CV3CDHJ6D0KEDHR6D24CD248MWKADHJ6WT34D25712KCD2474V46EA18H2M4GHM6WTK2E216S14CD238GSK0G9G692KCDHM6RW34CT16MV3CG9P60S34C1G70SMCHHQ8CVKJG9K6CVK6GHK70R46HJ26CR4AE9M8523ADHS8RR3EE1R74S32CHP6N1K0GT38D1M6C1R84TM2E9N8MSK2C1J71248E9H6H1MCD9J70VK4GSG6124CCHR6RS4ADSH8N0M4H1G84R4CD1G8D24AG9N6RR48DT1712K6GJ26X232DT36N0K4C9M8H236HJ48N2K4G9H8GVM8E1P8GSM6E9K891K4CSN65348C26611M8DHJ8S1M6H9G8H338CHS6GV3CD9K64S3GCHR8H2M6GJ58MT3EHA26S232GSJ6GTMAGA570W44DA2852KEDSR8MTKEGA460T3CCT18MR48CHK6WWKEGJ460WK4EA568VM6GSJ70T32CA461234DJ66RS34DHM6D242CT46MV3JDA584S4ADSM6S1MAE1P6GTKEGA68N1M8E216WRMAGHM6RR4ADSJ8MR3EDJ2690KAD9H6H346D9R88RKECSN8RRKJC1N74W34DSQ60W48DSJ8S1K0DSH8D1M4E1J6H1M2D1S8S33CG9R6RSMCH9K4CMGM81051JJ08SG64R30C1H4CMGM81054520A8A00")
        println(foo.toString(Charsets.UTF_8))
    }
}
