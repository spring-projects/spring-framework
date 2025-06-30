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
import org.springframework.core.codec.DecodingException
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.testfixture.codec.AbstractDecoderTests
import org.springframework.http.MediaType
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import reactor.test.StepVerifier.FirstStep
import java.math.BigDecimal
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.reflect.jvm.javaMethod

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
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Ordered::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, OrderedImpl::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClassWithGenerics(ArrayList::class.java, Int::class.java), MediaType.APPLICATION_PDF)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(Ordered::class.java), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(decoder.canDecode(ResolvableType.forClass(OrderedImpl::class.java), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.NONE, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(decoder.canDecode(ResolvableType.forClass(BigDecimal::class.java), MediaType.APPLICATION_JSON)).isFalse()

		assertThat(decoder.canDecode(ResolvableType.forClass(Pojo::class.java), MediaType.APPLICATION_NDJSON)).isTrue()
	}

	@Test
	override fun decode() {
		val input = Flux.concat(
			stringBuffer("{\"bar\":\"b1\",\"foo\":\"f1\"}\n"),
			stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}\n")
		)

		testDecodeAll(input, ResolvableType.forClass(Pojo::class.java), {
			it.expectNext(Pojo("f1", "b1"))
				.expectNext(Pojo("f2", "b2"))
				.expectComplete()
				.verify()
		}, null, null)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun polymorphicDecode() {
		val json = Json {
			serializersModule = SerializersModule {
				polymorphic(ISimpleSerializableBean::class, SimpleSerializableBean::class, SimpleSerializableBean.serializer())
			}
		}
		val customDecoder = KotlinSerializationJsonDecoder(json)
		val input = Flux.concat(
			stringBuffer("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonDecoderTests.SimpleSerializableBean\",\"name\":\"foo\"}\n"),
			stringBuffer("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonDecoderTests.SimpleSerializableBean\",\"name\":\"bar\"}\n"),
			stringBuffer("{\"type\":\"org.springframework.http.codec.json.KotlinSerializationJsonDecoderTests.SimpleSerializableBean\",\"name\":\"baz\"}\n")
		)

		val resolvableType = ResolvableType.forClass(ISimpleSerializableBean::class.java)
		val result: Flux<ISimpleSerializableBean> = customDecoder.decode(input, resolvableType, null, null) as Flux<ISimpleSerializableBean>
		val step: FirstStep<ISimpleSerializableBean> = StepVerifier.create(result)

		step.expectNext(SimpleSerializableBean("foo"))
			.expectNext(SimpleSerializableBean("bar"))
			.expectNext(SimpleSerializableBean("baz"))
			.verifyComplete()
	}

	@Test
	fun decodeWithUnexpectedFormat() {
		val input = Flux.concat(
			stringBuffer("{\"ba\":\"b1\",\"fo\":\"f1\"}\n"),
		)

		testDecode(input, ResolvableType.forClass(Pojo::class.java), { step: FirstStep<Pojo> ->
			step
				.expectError(DecodingException::class.java)
				.verify() }, null, null)
	}

	@Test
	fun decodeToMonoWithUnexpectedFormat() {
		val input = Flux.concat(
			stringBuffer("{\"ba\":\"b1\",\"fo\":\"f1\"}\n"),
		)

		testDecodeToMono(input, ResolvableType.forClass(Pojo::class.java), { step: FirstStep<Pojo> ->
			step.expectError(DecodingException::class.java)
				.verify() }, null, null)
	}

	@Test
	fun decodeStreamWithSingleBuffer() {
		val input = Flux.concat(
			stringBuffer("{\"bar\":\"b1\",\"foo\":\"f1\"}\n{\"bar\":\"b2\",\"foo\":\"f2\"}\n"),
		)

		testDecodeAll(input, ResolvableType.forClass(Pojo::class.java), {
			it.expectNext(Pojo("f1", "b1"))
				.expectNext(Pojo("f2", "b2"))
				.expectComplete()
				.verify()
		}, null, null)
	}

	@Test
	fun decodeStreamWithMultipleBuffersPerElement() {
		val input = Flux.concat(
			stringBuffer("{\"bar\":\"b1\","),
			stringBuffer("\"foo\":\"f1\"}\n"),
			stringBuffer("{\""),
			stringBuffer("bar\":\"b2\",\"foo\":\"f2\"}\n")
		)

		testDecodeAll(input, ResolvableType.forClass(Pojo::class.java), {
			it.expectNext(Pojo("f1", "b1"))
				.expectNext(Pojo("f2", "b2"))
				.expectComplete()
				.verify()
		}, null, null)
	}

	@Test
	override fun decodeToMono() {
		val input = Flux.concat(
				stringBuffer("[{\"bar\":\"b1\",\"foo\":\"f1\"},"),
				stringBuffer("{\"bar\":\"b2\",\"foo\":\"f2\"}]"))

		val elementType = ResolvableType.forClassWithGenerics(List::class.java, Pojo::class.java)

		testDecodeToMonoAll(input, elementType, {
			it.expectNext(listOf(Pojo("f1", "b1"), Pojo("f2", "b2")))
				.expectComplete()
				.verify()
		}, null, null)
	}

	@Test
	fun decodeToMonoWithNullableWithNull() {
		val input = Flux.concat(
			stringBuffer("{\"value\":null}\n"),
		)

		val methodParameter = MethodParameter.forExecutable(::handleMapWithNullable::javaMethod.get()!!, -1)
		val elementType = ResolvableType.forMethodParameter(methodParameter)

		testDecodeToMonoAll(input, elementType, {
			it.expectNext(mapOf("value" to null))
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

	fun handleMapWithNullable(map: Map<String, String?>) = map

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
