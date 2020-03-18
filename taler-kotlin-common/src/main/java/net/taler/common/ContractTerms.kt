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
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import net.taler.common.TalerUtils.getLocalizedString

@JsonInclude(NON_NULL)
abstract class Product {
    @get:JsonProperty("product_id")
    abstract val productId: String?
    abstract val description: String

    @get:JsonProperty("description_i18n")
    abstract val descriptionI18n: Map<String, String>?
    abstract val price: String

    @get:JsonProperty("delivery_location")
    abstract val location: String?
    abstract val image: String?

    @get:JsonIgnore
    val localizedDescription: String
        @RequiresApi(26)
        get() = getLocalizedString(descriptionI18n, description)
}

data class ContractProduct(
    override val productId: String?,
    override val description: String,
    override val descriptionI18n: Map<String, String>?,
    override val price: String,
    override val location: String?,
    override val image: String?,
    val quantity: Int
) : Product()

@JsonInclude(NON_EMPTY)
class Timestamp(
    @JsonProperty("t_ms")
    val ms: Long
)
