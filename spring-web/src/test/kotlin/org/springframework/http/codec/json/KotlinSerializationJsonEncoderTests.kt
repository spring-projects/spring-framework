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

package org.springframework.http.codec.json

import kotlinx.serialization.Serializable
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
import java.nio.charset.StandardCharsets

/**
 * Tests for the JSON encoding using kotlinx.serialization.
 *
 * @author Sebastien Deleuze
 */
@Suppress("UsePropertyAccessSyntax")
class KotlinSerializationJsonEncoderTests : AbstractEncoderTests<KotlinSerializationJsonEncoder>(KotlinSerializationJsonEncoder()) {

	@Test
	override fun canEncode() {
		val pojoType = ResolvableType.forClass(Pojo::class.java)
		val jsonSubtype = MediaType("application", "vnd.test-micro-type+json")
		assertThat(encoder.canEncode(pojoType, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(pojoType, jsonSubtype)).isTrue()
		assertThat(encoder.canEncode(pojoType, null)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java),
				MediaType("application", "json", StandardCharsets.UTF_8))).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java),
				MediaType("application", "json", StandardCharsets.US_ASCII))).isTrue()

		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
	}

	@Test
	override fun encode() {
		val input = Flux.just(
				Pojo("foo", "bar"),
				Pojo("foofoo", "barbar"),
				Pojo("foofoofoo", "barbarbar")
		)
		testEncode(input, Pojo::class.java, { step: FirstStep<DataBuffer?> -> step
			.consumeNextWith(expectString("[" +
					"{\"foo\":\"foo\",\"bar\":\"bar\"}," +
					"{\"foo\":\"foofoo\",\"bar\":\"barbar\"}," +
					"{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}]")
				.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
			.verifyComplete()
		})
	}

	@Test
	fun encodeMono() {
		val input = Mono.just(Pojo("foo", "bar"))
		testEncode(input, Pojo::class.java, { step: FirstStep<DataBuffer?> -> step
			.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}")
				.andThen { dataBuffer: DataBuffer? -> DataBufferUtils.release(dataBuffer) })
			.verifyComplete()
		})
	}

	@Test
	fun canNotEncode() {
		assertThat(encoder.canEncode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		val sseType = ResolvableType.forClass(ServerSentEvent::class.java)
		assertThat(encoder.canEncode(sseType, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Ordered::class.java), MediaType.APPLICATION_JSON)).isFalse()
	}


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

}
