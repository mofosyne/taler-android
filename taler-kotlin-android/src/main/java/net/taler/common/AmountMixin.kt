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

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer

/**
 * Used to support Jackson serialization along with KotlinX.
 */
@JsonSerialize(using = AmountSerializer::class)
@JsonDeserialize(using = AmountDeserializer::class)
abstract class AmountMixin

class AmountSerializer : StdSerializer<Amount>(Amount::class.java) {
    override fun serialize(value: Amount, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.toJSONString())
    }
}

class AmountDeserializer : StdDeserializer<Amount>(Amount::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Amount {
        val node = p.codec.readValue(p, String::class.java)
        try {
            return Amount.fromJSONString(node)
        } catch (e: AmountParserException) {
            throw JsonMappingException(p, "Error parsing Amount", e)
        }
    }
}
