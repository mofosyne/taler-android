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

package net.taler.wallet.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import net.taler.wallet.Amount


@JsonIgnoreProperties(ignoreUnknown = true)
data class ContractTerms(
    val summary: String,
    val products: List<ContractProduct>,
    val amount: Amount
)

interface Product {
    val id: String?
    val description: String
    val price: Amount
    val location: String?
    val image: String?
}

@JsonIgnoreProperties("totalPrice")
data class ContractProduct(
    @JsonProperty("product_id")
    override val id: String?,
    override val description: String,
    override val price: Amount,
    @JsonProperty("delivery_location")
    override val location: String?,
    override val image: String?,
    val quantity: Int
) : Product {

    val totalPrice: Amount by lazy {
        val amount = price.amount.toDouble() * quantity
        Amount(price.currency, amount.toString())
    }

}
