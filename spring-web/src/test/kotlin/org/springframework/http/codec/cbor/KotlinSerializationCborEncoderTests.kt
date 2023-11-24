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

package org.springframework.http.codec.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier.FirstStep

import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.testfixture.codec.AbstractEncoderTests
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent

/**
 * Tests for the JSON encoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 */
@ExperimentalSerializationApi
class KotlinSerializationCborEncoderTests : AbstractEncoderTests<KotlinSerializationCborEncoder>(KotlinSerializationCborEncoder()) {

	@Test
	override fun canEncode() {
		val pojoType = ResolvableType.forClass(Pojo::class.java)
		assertThat(encoder.canEncode(pojoType, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(encoder.canEncode(pojoType, null)).isTrue()

		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
	}

	@Test
	override fun encode() {
		val pojo1 = Pojo("foo", "bar")
		val pojo2 = Pojo("foofoo", "barbar")
		val pojo3 = Pojo("foofoofoo", "barbarbar")
		val input = Flux.just(
				pojo1,
				pojo2,
				pojo3
		)
		val pojoBytes = Cbor.Default.encodeToByteArray(arrayOf(pojo1, pojo2, pojo3))
		testEncode(input, Pojo::class.java) { step: FirstStep<DataBuffer?> ->
			step
				.consumeNextWith(expectBytes(pojoBytes)
					.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
				.verifyComplete()
		}
	}

	@Test
	fun encodeMono() {
		val pojo = Pojo("foo", "bar")
		val input = Mono.just(pojo)
		testEncode(input, Pojo::class.java) { step: FirstStep<DataBuffer?> ->
			step
				.consumeNextWith(expectBytes(Cbor.Default.encodeToByteArray(pojo))
					.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
				.verifyComplete()
		}
	}

	@Test
	fun canNotEncode() {
		assertThat(encoder.canEncode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		val sseType = ResolvableType.forClass(ServerSentEvent::class.java)
		assertThat(encoder.canEncode(sseType, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Ordered::class.java), MediaType.APPLICATION_CBOR)).isFalse()
	}


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

}
