/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.protobuf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.testfixture.codec.AbstractEncoderTests
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier.FirstStep

/**
 * Tests for the JSON encoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 */
@Suppress("UsePropertyAccessSyntax")
@ExperimentalSerializationApi
class KotlinSerializationProtobufEncoderTests : AbstractEncoderTests<KotlinSerializationProtobufEncoder>(KotlinSerializationProtobufEncoder()) {

	private val mediaTypes = listOf(
		MediaType.APPLICATION_PROTOBUF,
		MediaType.APPLICATION_OCTET_STREAM,
		MediaType("application", "vnd.google.protobuf")
	)

	@Test
	override fun canEncode() {
		val pojoType = ResolvableType.forClass(Pojo::class.java)
		assertThat(encoder.canEncode(pojoType, null)).isTrue()

		for (mimeType in ProtobufCodecSupport.MIME_TYPES) {
			val mediaType = MediaType(mimeType)
			assertThat(encoder.canEncode(pojoType, mediaType)).isTrue()

			assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), mimeType)).isTrue()
			assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), mimeType)).isFalse()
			assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), mimeType)).isTrue()
			assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), mimeType)).isTrue()
		}
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
		val pojoBytes = ProtoBuf.Default.encodeToByteArray(arrayOf(pojo1, pojo2, pojo3))
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
				.consumeNextWith(expectBytes(ProtoBuf.Default.encodeToByteArray(pojo))
					.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
				.verifyComplete()
		}
	}

	@Test
	fun canNotEncode() {
		assertThat(encoder.canEncode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		val sseType = ResolvableType.forClass(ServerSentEvent::class.java)
		for (mediaType in mediaTypes) {
			assertThat(encoder.canEncode(sseType, mediaType)).isFalse()
			assertThat(encoder.canEncode(ResolvableType.forClass(Ordered::class.java), mediaType)).isFalse()
		}
	}


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

}
