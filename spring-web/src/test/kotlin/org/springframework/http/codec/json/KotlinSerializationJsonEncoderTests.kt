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

package org.springframework.http.codec.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractEncoderTests
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.javaMethod

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
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()

		assertThat(encoder.canEncode(pojoType, MediaType.APPLICATION_NDJSON)).isTrue()
	}

	@Test
	override fun encode() {
		val input = Flux.just(
			Pojo("foo", "bar"),
			Pojo("foofoo", "barbar"),
			Pojo("foofoofoo", "barbarbar")
		)
		testEncode(input, Pojo::class.java) {
			it.consumeNextWith(expectString("[{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoo\",\"bar\":\"barbar\"}"))
				.consumeNextWith(expectString(",{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}"))
				.consumeNextWith(expectString("]"))
				.verifyComplete()
		}
	}

	@Test
	fun encodeEmpty() {
		testEncode(Flux.empty(), Pojo::class.java) {
			it
				.consumeNextWith(expectString("["))
				.consumeNextWith(expectString("]"))
				.verifyComplete()
		}
	}

	@Test
	fun encodeWithErrorAsFirstSignal() {
		val message = "I'm a teapot"
		val input = Flux.error<Any>(IllegalStateException(message))
		val output = encoder.encode(input, this.bufferFactory, ResolvableType.forClass(Pojo::class.java), null, null)
		StepVerifier.create(output).expectErrorMessage(message).verify()
	}

	@Test
	fun encodeStream() {
		val input = Flux.just(
			Pojo("foo", "bar"),
			Pojo("foofoo", "barbar"),
			Pojo("foofoofoo", "barbarbar")
		)
		testEncodeAll(
			input,
			ResolvableType.forClass(Pojo::class.java),
			MediaType.APPLICATION_NDJSON,
			null
		) {
			it.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoo\",\"bar\":\"barbar\"}\n"))
				.consumeNextWith(expectString("{\"foo\":\"foofoofoo\",\"bar\":\"barbarbar\"}\n"))
				.verifyComplete()
		}
	}

	@Test
	fun encodePolymorphicStream() {
		val json = Json {
			serializersModule = SerializersModule {
				polymorphic(ISimpleSerializableBean::class, SimpleSerializableBean::class, SimpleSerializableBean.serializer())
			}
		}
		val customEncoder = KotlinSerializationJsonEncoder(json)
		val input = Flux<ISimpleSerializableBean>.just(
			SimpleSerializableBean("foo"),
			SimpleSerializableBean("bar"),
			SimpleSerializableBean("baz")
		)
		val result = customEncoder.encode(input, this.bufferFactory, ResolvableType.forClass(ISimpleSerializableBean::class.java), MediaType.APPLICATION_NDJSON, null)
		val step = StepVerifier.create<DataBuffer>(result)
		step
			.consumeNextWith { expectString("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonEncoderTests.SimpleSerializableBean\",\"name\":\"foo\"}\n").accept(it) }
			.consumeNextWith { expectString("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonEncoderTests.SimpleSerializableBean\",\"name\":\"bar\"}\n").accept(it) }
			.consumeNextWith { expectString("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonEncoderTests.SimpleSerializableBean\",\"name\":\"baz\"}\n").accept(it) }
			.verifyComplete()
	}

	@Test
	fun encodeMono() {
		val input = Mono.just(Pojo("foo", "bar"))
		testEncode(input, Pojo::class.java) {
			it.consumeNextWith(expectString("{\"foo\":\"foo\",\"bar\":\"bar\"}"))
				.verifyComplete()
		}
	}

	@Test
	fun encodeMonoWithNullableWithNull() {
		val input = Mono.just(mapOf("value" to null))
		val methodParameter = MethodParameter.forExecutable(::handleMapWithNullable::javaMethod.get()!!, -1)
		testEncode(input, ResolvableType.forMethodParameter(methodParameter), null, null) {
			it.consumeNextWith(expectString("{\"value\":null}"))
				.verifyComplete()
		}
	}

	@Test
	fun canNotEncode() {
		assertThat(encoder.canEncode(ResolvableType.forClass(String::class.java), null)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_XML)).isFalse()
		val sseType = ResolvableType.forClass(ServerSentEvent::class.java)
		assertThat(encoder.canEncode(sseType, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClass(BigDecimal::class.java), null)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(List::class.java, OrderedImpl::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
		assertThat(encoder.canEncode(ResolvableType.NONE, MediaType.APPLICATION_JSON)).isFalse()
	}

	@Test
	fun encodeProperty() {
		val input = Mono.just(value)
		val method = this::class.java.getDeclaredMethod("getValue")
		val methodParameter = MethodParameter.forExecutable(method, -1)
		testEncode(input, ResolvableType.forMethodParameter(methodParameter), null, null) {
			it.consumeNextWith(expectString("42"))
				.verifyComplete()
		}
	}


	@Serializable
	data class Pojo(val foo: String, val bar: String, val pojo: Pojo? = null)

	fun handleMapWithNullable(map: Map<String, String?>) = map

	val value: Int
		get() = 42

	interface ISimpleSerializableBean {
		val name: String
	}

	@Serializable
	data class SimpleSerializableBean(override val name: String): ISimpleSerializableBean

	class OrderedImpl : Ordered {
		override fun getOrder(): Int {
			return 0
		}
	}

}
