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

import android.net.Uri
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.taler.common.Amount
import net.taler.common.ContractProduct
import net.taler.common.Product
import net.taler.common.TalerUtils
import java.util.*

data class Config(
    val configUrl: String,
    val username: String,
    val password: String
) {
    fun isValid() = !configUrl.isBlank()
    fun hasPassword() = !password.isBlank()
}

data class MerchantConfig(
    @JsonProperty("base_url")
    val baseUrl: String,
    val instance: String,
    @JsonProperty("api_key")
    val apiKey: String,
    val currency: String?
) {
    fun urlFor(endpoint: String, params: Map<String, String>?): String {
        val uriBuilder = Uri.parse(baseUrl).buildUpon()
        uriBuilder.appendPath(endpoint)
        params?.forEach {
            uriBuilder.appendQueryParameter(it.key, it.value)
        }
        return uriBuilder.toString()
    }
}

data class Category(
    val id: Int,
    val name: String,
    @JsonProperty("name_i18n")
    val nameI18n: Map<String, String>?
) {
    var selected: Boolean = false
    val localizedName: String get() = TalerUtils.getLocalizedString(nameI18n, name)
}

data class ConfigProduct(
    @JsonIgnore
    val id: String = UUID.randomUUID().toString(),
    override val productId: String?,
    override val description: String,
    override val descriptionI18n: Map<String, String>?,
    override val price: String,
    override val location: String?,
    override val image: String?,
    val categories: List<Int>,
    @JsonIgnore
    val quantity: Int = 0
) : Product() {
    val priceAsDouble by lazy { Amount.fromString(price).amount.toDouble() }

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
