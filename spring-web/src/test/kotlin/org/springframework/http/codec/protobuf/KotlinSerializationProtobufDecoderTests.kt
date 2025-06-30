/*
 * Copyright 2002-present the original author or authors.
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
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.codec.DecodingException
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractDecoderTests
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.StepVerifier.FirstStep

/**
 * Tests for the JSON decoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 * @author Iain Henderson
 */
@ExperimentalSerializationApi
class KotlinSerializationProtobufDecoderTests : AbstractDecoderTests<KotlinSerializationProtobufDecoder>(KotlinSerializationProtobufDecoder()) {

	@Test
	override fun canDecode() {
		for (mimeType in listOf(MediaType.APPLICATION_PROTOBUF, MediaType.APPLICATION_OCTET_STREAM, MediaType("application", "vnd.google.protobuf"))) {
			assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java),mimeType)).isTrue()

			assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), mimeType)).isTrue()
			assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), mimeType)).isTrue()
			assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, OrderedImpl::class.java), mimeType)).isFalse()
			assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), mimeType)).isTrue()
			assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), mimeType)).isTrue()
			assertThat(decoder.canDecode(ResolvableType.forClass(Ordered::class.java), mimeType)).isTrue()
			assertThat(decoder.canDecode(ResolvableType.forClass(OrderedImpl::class.java), mimeType)).isFalse()
		}
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), null)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
	}

	@Test
	override fun decode() {
		val output = decoder.decode(Mono.empty(), ResolvableType.forClass(Pojo::class.java), null, emptyMap())
		StepVerifier
				.create(output)
				.expectError(UnsupportedOperationException::class.java)
				.verify();
	}

	@Test
	override fun decodeToMono() {
		val pojo1 = Pojo("f1", "b1")
		val pojo2 = Pojo("f2", "b2")
		val input = Flux.concat(
			byteBuffer(pojo1),
			byteBuffer(pojo2)
		)

		val elementType = ResolvableType.forClass(Pojo::class.java)

		testDecodeToMonoAll(input, elementType, { step: FirstStep<Any> ->
			step
					.expectNext(pojo2)
					.expectComplete()
					.verify()
		}, null, null)
	}

	@Test
	fun decodeToMonoWithUnexpectedFormat() {
		val input = Mono.just(
			bufferFactory.allocateBuffer(0),
		)

		val elementType = ResolvableType.forClass(Pojo::class.java)

		testDecodeToMono(input, elementType, { step: FirstStep<Any> ->
			step
				.expectError(DecodingException::class.java)
				.verify()
		}, null, null)
	}

	private fun byteBuffer(value: Any): Mono<DataBuffer> {
		return Mono.defer {
			val bytes = ProtoBuf.Default.encodeToByteArray(serializer(Pojo::class.java), value)
			val buffer = bufferFactory.allocateBuffer(bytes.size)
			buffer.write(bytes)
			Mono.just(buffer)
		}
	}


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

	class OrderedImpl : Ordered {
		override fun getOrder(): Int {
			return 0
		}
	}

}
