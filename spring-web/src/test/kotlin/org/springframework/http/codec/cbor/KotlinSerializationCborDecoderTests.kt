/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.codec.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.StepVerifier.FirstStep

import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractDecoderTests
import org.springframework.http.MediaType

/**
 * Tests for the JSON decoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 */
@ExperimentalSerializationApi
class KotlinSerializationCborDecoderTests : AbstractDecoderTests<KotlinSerializationCborDecoder>(KotlinSerializationCborDecoder()) {

	@Suppress("UsePropertyAccessSyntax", "DEPRECATION")
	@Test
	override fun canDecode() {
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), null)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()

		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Ordered::class.java), MediaType.APPLICATION_CBOR)).isFalse()
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
		val pojo1 = Pojo("f1", "b1")
		val input = Flux.concat(
			byteBuffer(pojo1)
		)

		val elementType = ResolvableType.forClass(Pojo::class.java)

		testDecodeToMonoAll(input, elementType, { step: FirstStep<Any> ->
			step
					.expectNext(pojo1)
					.expectComplete()
					.verify()
		}, null, null)
	}

	private fun byteBuffer(value: Any): Mono<DataBuffer> {
		return Mono.defer {
			val bytes = Cbor.Default.encodeToByteArray(serializer(Pojo::class.java), value)
			val buffer = bufferFactory.allocateBuffer(bytes.size)
			buffer.write(bytes)
			Mono.just(buffer)
		}
	}

}
@Serializable
data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)
