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

import androidx.annotation.RequiresApi
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.TalerUtils.getLocalizedString
import net.taler.lib.common.Amount
import net.taler.lib.common.Timestamp

@Serializable
data class ContractTerms(
    val summary: String,
    @SerialName("summary_i18n")
    @get:JsonProperty("summary_i18n")
    val summaryI18n: Map<String, String>? = null,
    val amount: Amount,
    @SerialName("fulfillment_url")
    @get:JsonProperty("fulfillment_url")
    val fulfillmentUrl: String,
    val products: List<ContractProduct>,
    @SerialName("wire_transfer_deadline")
    @get:JsonProperty("wire_transfer_deadline")
    val wireTransferDeadline: Timestamp? = null,
    @SerialName("refund_deadline")
    @get:JsonProperty("refund_deadline")
    val refundDeadline: Timestamp? = null
)

@JsonInclude(NON_NULL)
abstract class Product {
    @get:JsonProperty("product_id")
    abstract val productId: String?
    abstract val description: String

    @get:JsonProperty("description_i18n")
    abstract val descriptionI18n: Map<String, String>?
    abstract val price: Amount

    @get:JsonProperty("delivery_location")
    abstract val location: String?
    abstract val image: String?

    @get:JsonIgnore
    val localizedDescription: String
        @RequiresApi(26)
        get() = getLocalizedString(descriptionI18n, description)
}

@Serializable
data class ContractProduct(
    @SerialName("product_id")
    override val productId: String? = null,
    override val description: String,
    @SerialName("description_i18n")
    override val descriptionI18n: Map<String, String>? = null,
    override val price: Amount,
    @SerialName("delivery_location")
    override val location: String? = null,
    override val image: String? = null,
    val quantity: Int
) : Product() {
    @get:JsonIgnore
    val totalPrice: Amount by lazy {
        price * quantity
    }
}

data class ContractMerchant(
    val name: String
)
