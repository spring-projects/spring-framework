/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.json

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractDecoderTests
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.StepVerifier.FirstStep
import java.lang.UnsupportedOperationException
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Tests for the JSON decoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 */
class KotlinSerializationJsonDecoderTests : AbstractDecoderTests<KotlinSerializationJsonDecoder>(KotlinSerializationJsonDecoder()) {

	@Test
	override fun canDecode() {
		val jsonSubtype = MediaType("application", "vnd.test-micro-type+json")
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), jsonSubtype)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), null)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java),
				MediaType("application", "json", StandardCharsets.UTF_8))).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java),
				MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java),
				MediaType("application", "json", StandardCharsets.ISO_8859_1))).isTrue()

		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Ordered::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.NONE, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(BigDecimal::class.java), MediaType.APPLICATION_JSON)).isFalse()
	}

	@Test
	override fun decode() {
		val output = decoder.decode(Mono.empty(), ResolvableType.forClass(Pojo::class.java), null, emptyMap())
		StepVerifier
				.create(output)
				.expectError(UnsupportedOperationException::class.java)
	}

	@Test
	override fun decodeToMono() {
		val input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"))

		val elementType = ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java)

		testDecodeToMonoAll(input, elementType, { step: FirstStep<Any> ->
			step
					.expectNext(listOf(Pojo("f1", "b1"), Pojo("f2", "b2")))
					.expectComplete()
					.verify()
		}, null, null)
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


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

}
