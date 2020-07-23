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

package net.taler.merchantpos.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.taler.common.Amount
import net.taler.common.ContractProduct
import net.taler.common.Product
import net.taler.common.TalerUtils
import java.util.UUID

data class Config(
    val configUrl: String,
    val username: String,
    val password: String
) {
    fun isValid() = !configUrl.isBlank()
    fun hasPassword() = !password.isBlank()
}

@Serializable
data class PosConfig(
    @SerialName("config")
    val merchantConfig: net.taler.merchantlib.MerchantConfig,
    val categories: List<Category>,
    val products: List<ConfigProduct>
)

@Serializable
data class Category(
    val id: Int,
    val name: String,
    @SerialName("name_i18n")
    val nameI18n: Map<String, String>? = null
) {
    var selected: Boolean = false
    val localizedName: String get() = TalerUtils.getLocalizedString(nameI18n, name)
}

@Serializable
data class ConfigProduct(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("product_id")
    override val productId: String? = null,
    override val description: String,
    @SerialName("description_i18n")
    override val descriptionI18n: Map<String, String>? = null,
    override val price: Amount,
    @SerialName("delivery_location")
    override val location: String? = null,
    override val image: String? = null,
    val categories: List<Int>,
    val quantity: Int = 0
) : Product() {
    val totalPrice by lazy { price * quantity }

    fun toContractProduct() = ContractProduct(
        productId = productId,
        description = description,
        descriptionI18n = descriptionI18n,
        price = price,
        location = location,
        image = image,
        quantity = quantity
    )

    override fun equals(other: Any?) = other is ConfigProduct && id == other.id
    override fun hashCode() = id.hashCode()
}
