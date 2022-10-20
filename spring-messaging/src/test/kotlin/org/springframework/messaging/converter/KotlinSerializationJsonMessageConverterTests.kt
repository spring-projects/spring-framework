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

package org.springframework.messaging.converter

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.messaging.support.MessageBuilder
import java.nio.charset.StandardCharsets
import kotlin.reflect.typeOf

@Suppress("UsePropertyAccessSyntax")
class KotlinSerializationJsonMessageConverterTests {

	private val converter = KotlinSerializationJsonMessageConverter()

	@Test
	fun readObject() {
		val payload = """
			{
				"bytes": [
					1,
					2
				],
				"array": [
					"Foo",
					"Bar"
				],
				"number": 42,
				"string": "Foo",
				"bool": true,
				"fraction": 42
			}
		""".trimIndent()
		val message = MessageBuilder.withPayload(payload.toByteArray(StandardCharsets.UTF_8)).build()
		val result = converter.fromMessage(message, SerializableBean::class.java) as SerializableBean

		assertThat(result.bytes).containsExactly(0x1, 0x2)
		assertThat(result.array).containsExactly("Foo", "Bar")
		assertThat(result.number).isEqualTo(42)
		assertThat(result.string).isEqualTo("Foo")
		assertThat(result.bool).isTrue()
		assertThat(result.fraction).isEqualTo(42.0f)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun readArrayOfObjects() {
		val payload = """
			[
				{
					"bytes": [
						1,
						2
					],
					"array": [
						"Foo",
						"Bar"
					],
					"number": 42,
					"string": "Foo",
					"bool": true,
					"fraction": 42
				}
			]
		""".trimIndent()
		val message = MessageBuilder.withPayload(payload.toByteArray(StandardCharsets.UTF_8)).build()
		val result = converter.fromMessage(message, Array<SerializableBean>::class.java) as Array<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(0x1, 0x2)
		assertThat(result[0].array).containsExactly("Foo", "Bar")
		assertThat(result[0].number).isEqualTo(42)
		assertThat(result[0].string).isEqualTo("Foo")
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(42.0f)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	@ExperimentalStdlibApi
	fun readGenericCollection() {
		val payload = """
			[
				{
					"bytes": [
						1,
						2
					],
					"array": [
						"Foo",
						"Bar"
					],
					"number": 42,
					"string": "Foo",
					"bool": true,
					"fraction": 42
				}
			]
		""".trimIndent()
		val method = javaClass.getDeclaredMethod("handleList", List::class.java)
		val param = MethodParameter(method, 0)
		val message = MessageBuilder.withPayload(payload.toByteArray(StandardCharsets.UTF_8)).build()
		val result = converter.fromMessage(message, typeOf<List<SerializableBean>>()::class.java, param) as List<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(0x1, 0x2)
		assertThat(result[0].array).containsExactly("Foo", "Bar")
		assertThat(result[0].number).isEqualTo(42)
		assertThat(result[0].string).isEqualTo("Foo")
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(42.0f)
	}

	@Test
	fun readFailsOnInvalidJson() {
		val payload = """
			this is an invalid JSON document
		""".trimIndent()

		val message = MessageBuilder.withPayload(payload.toByteArray(StandardCharsets.UTF_8)).build()
		assertThatExceptionOfType(MessageConversionException::class.java).isThrownBy {
			converter.fromMessage(message, SerializableBean::class.java)
		}
	}

	@Test
	fun writeObject() {
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)
		val message = converter.toMessage(serializableBean, null)
		val result = String((message!!.payload as ByteArray), StandardCharsets.UTF_8)

		assertThat(result)
				.contains("\"bytes\":[1,2]")
				.contains("\"array\":[\"Foo\",\"Bar\"]")
				.contains("\"number\":42")
				.contains("\"string\":\"Foo\"")
				.contains("\"bool\":true")
				.contains("\"fraction\":42.0")
	}

	@Test
	fun writeObjectWithNullableProperty() {
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, null, true, 42.0f)
		val message = converter.toMessage(serializableBean, null)
		val result = String((message!!.payload as ByteArray), StandardCharsets.UTF_8)

		assertThat(result)
				.contains("\"bytes\":[1,2]")
				.contains("\"array\":[\"Foo\",\"Bar\"]")
				.contains("\"number\":42")
				.contains("\"string\":null")
				.contains("\"bool\":true")
				.contains("\"fraction\":42.0")
	}

	@Test
	fun writeArrayOfObjects() {
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)
		val expectedJson = """
			[{"bytes":[1,2],"array":["Foo","Bar"],"number":42,"string":"Foo","bool":true,"fraction":42.0}]
		""".trimIndent()

		val message = converter.toMessage(arrayOf(serializableBean), null)
		val result = String((message!!.payload as ByteArray), StandardCharsets.UTF_8)

		assertThat(result).isEqualTo(expectedJson)
	}

	@Test
	@ExperimentalStdlibApi
	fun writeGenericCollection() {
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)
		val expectedJson = """
			[{"bytes":[1,2],"array":["Foo","Bar"],"number":42,"string":"Foo","bool":true,"fraction":42.0}]
		""".trimIndent()

		val method = javaClass.getDeclaredMethod("handleList", List::class.java)
		val param = MethodParameter(method, 0)
		val message = converter.toMessage(arrayListOf(serializableBean), null, param)
		val result = String((message!!.payload as ByteArray), StandardCharsets.UTF_8)

		assertThat(result).isEqualTo(expectedJson)
	}

	@Suppress("UNUSED_PARAMETER")
	fun handleList(payload: List<SerializableBean>) {}

	@Serializable
	@Suppress("ArrayInDataClass")
	data class SerializableBean(
			val bytes: ByteArray,
			val array: Array<String>,
			val number: Int,
			val string: String?,
			val bool: Boolean,
			val fraction: Float
	)
}
