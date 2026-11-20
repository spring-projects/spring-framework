/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.json

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractDecoderTests
import org.springframework.http.MediaType
import org.springframework.http.customJson
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Tests for the JSON decoding using kotlinx.serialization with a custom serializer module.
 *
 * @author Sebastien Deleuze
 */
class CustomKotlinSerializationJsonDecoderTests :
		AbstractDecoderTests<KotlinSerializationJsonDecoder>(KotlinSerializationJsonDecoder(customJson) { true }) {

	@Test
	override fun canDecode() {
		val bigDecimalType = ResolvableType.forClass(BigDecimal::class.java)
		Assertions.assertThat(decoder.canDecode(bigDecimalType, MediaType.APPLICATION_JSON)).isTrue()
	}

	@Test
	override fun decode() {
		val input = Flux.concat(
			stringBuffer("1.0\n"),
			stringBuffer("2.0\n")
		)
		val output = decoder.decode(input, ResolvableType.forClass(BigDecimal::class.java), null, emptyMap())
		StepVerifier
				.create(output)
				.expectNext(BigDecimal.valueOf(1.0))
				.expectNext(BigDecimal.valueOf(2.0))
				.verifyComplete()
	}

	@Test
	override fun decodeToMono() {
		val input = stringBuffer("1.0")
		val output = decoder.decodeToMono(input,
				ResolvableType.forClass(BigDecimal::class.java), null, emptyMap())
		StepVerifier
				.create(output)
				.expectNext(BigDecimal.valueOf(1.0))
				.expectComplete()
				.verify()
	}

	private fun stringBuffer(value: String): Mono<DataBuffer> {
		return stringBuffer(value, StandardCharsets.UTF_8)
	}

	private fun stringBuffer(value: String, charset: Charset): Mono<DataBuffer> {
		return Mono.defer {
			val bytes = value.toByteArray(charset)
			val buffer = bufferFactory.allocateBuffer(bytes.size)
			buffer.write(bytes)
			Mono.just(buffer)
		}
	}

}
