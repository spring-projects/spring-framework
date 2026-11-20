/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import java.math.BigDecimal

object BigDecimalSerializer : KSerializer<BigDecimal> {
	override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.DOUBLE)

	override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal.valueOf(decoder.decodeDouble())

	override fun serialize(encoder: Encoder, value: BigDecimal) {
		encoder.encodeDouble(value.toDouble())
	}
}

val customJson = Json {
	serializersModule = SerializersModule {
		contextual(BigDecimal::class, BigDecimalSerializer)
	}
}
