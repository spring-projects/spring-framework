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

package org.springframework.http.converter.json

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.customJson
import org.springframework.web.testfixture.http.MockHttpInputMessage
import org.springframework.web.testfixture.http.MockHttpOutputMessage
import java.math.BigDecimal

/**
 * Tests for the JSON conversion using kotlinx.serialization.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 */
@Suppress("UsePropertyAccessSyntax")
class KotlinSerializationJsonHttpMessageConverterTests {

	private val converter = KotlinSerializationJsonHttpMessageConverter()

	@Test
	fun canReadJson() {
		assertThat(converter.canRead(SerializableBean::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(SerializableBean::class.java, MediaType.APPLICATION_PDF)).isFalse()
		assertThat(converter.canRead(String::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(NotSerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(Map::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(typeTokenOf<Map<String, SerializableBean>>(), Map::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(List::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(typeTokenOf<List<SerializableBean>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(Set::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(typeTokenOf<Set<SerializableBean>>(), Set::class.java, MediaType.APPLICATION_JSON)).isTrue()

		assertThat(converter.canRead(typeTokenOf<List<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(typeTokenOf<ArrayList<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(typeTokenOf<List<Int>>(), List::class.java, MediaType.APPLICATION_PDF)).isFalse()

		assertThat(converter.canRead(typeTokenOf<Ordered>(), Ordered::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(typeTokenOf<List<Ordered>>(), List::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(ResolvableType.NONE.type, null, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(BigDecimal::class.java, null, MediaType.APPLICATION_JSON)).isFalse()
	}

	@Test
	fun canWriteJson() {
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_PDF)).isFalse()
		assertThat(converter.canWrite(String::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(NotSerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(Map::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canWrite(typeTokenOf<Map<String, SerializableBean>>(), Map::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(List::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canWrite(typeTokenOf<List<SerializableBean>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(Set::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canWrite(typeTokenOf<Set<SerializableBean>>(), Set::class.java, MediaType.APPLICATION_JSON)).isTrue()

		assertThat(converter.canWrite(typeTokenOf<List<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(typeTokenOf<ArrayList<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(typeTokenOf<List<Int>>(), List::class.java, MediaType.APPLICATION_PDF)).isFalse()

		assertThat(converter.canWrite(typeTokenOf<Ordered>(), Ordered::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(ResolvableType.NONE.type, SerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(BigDecimal::class.java, BigDecimal::class.java, MediaType.APPLICATION_JSON)).isFalse()
	}

	@Test
	fun canReadMicroformats() {
		val jsonSubtype = MediaType("application", "vnd.test-micro-type+json")
		assertThat(converter.canRead(SerializableBean::class.java, jsonSubtype)).isTrue()
	}

	@Test
	fun canWriteMicroformats() {
		val jsonSubtype = MediaType("application", "vnd.test-micro-type+json")
		assertThat(converter.canWrite(SerializableBean::class.java, jsonSubtype)).isTrue()
	}

	@Test
	fun readObject() {
		val body = """
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
		val inputMessage = MockHttpInputMessage(body.toByteArray(charset("UTF-8")))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val result = converter.read(SerializableBean::class.java, inputMessage) as SerializableBean

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
		val body = """
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
		val inputMessage = MockHttpInputMessage(body.toByteArray(charset("UTF-8")))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val result = converter.read(Array<SerializableBean>::class.java, inputMessage) as Array<SerializableBean>

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
		val body = """
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
		val inputMessage = MockHttpInputMessage(body.toByteArray(charset("UTF-8")))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val result = converter.read(typeOf<List<SerializableBean>>().javaType, null, inputMessage)
				as List<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(0x1, 0x2)
		assertThat(result[0].array).containsExactly("Foo", "Bar")
		assertThat(result[0].number).isEqualTo(42)
		assertThat(result[0].string).isEqualTo("Foo")
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(42.0f)
	}

	@Test
	fun readObjectInUtf16() {
		val body = "\"H\u00e9llo W\u00f6rld\""
		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_16BE))
		inputMessage.headers.contentType = MediaType("application", "json", StandardCharsets.UTF_16BE)

		val result = this.converter.read(String::class.java, inputMessage)

		assertThat(result).isEqualTo("H\u00e9llo W\u00f6rld")
	}

	@Test
	fun readFailsOnInvalidJson() {
		val body = """
			this is an invalid JSON document
		""".trimIndent()

		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_8))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		assertThatExceptionOfType(HttpMessageNotReadableException::class.java).isThrownBy {
			converter.read(SerializableBean::class.java, inputMessage)
		}
	}

	@Test
	fun writeObject() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)

		this.converter.write(serializableBean, null, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/json"))
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
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, null, true, 42.0f)

		this.converter.write(serializableBean, null, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/json"))
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
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)
		val expectedJson = """
			[{"bytes":[1,2],"array":["Foo","Bar"],"number":42,"string":"Foo","bool":true,"fraction":42.0}]
		""".trimIndent()

		this.converter.write(arrayOf(serializableBean), null, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/json"))
		assertThat(result).isEqualTo(expectedJson)
	}

	@Test
	@ExperimentalStdlibApi
	fun writeGenericCollection() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)
		val expectedJson = """
			[{"bytes":[1,2],"array":["Foo","Bar"],"number":42,"string":"Foo","bool":true,"fraction":42.0}]
		""".trimIndent()

		this.converter.write(arrayListOf(serializableBean), typeOf<List<SerializableBean>>().javaType, null,
				outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/json"))
		assertThat(result).isEqualTo(expectedJson)
	}

	@Test
	fun writeObjectInUtf16() {
		val outputMessage = MockHttpOutputMessage()
		val utf16 = "H\u00e9llo W\u00f6rld"
		val contentType = MediaType("application", "json", StandardCharsets.UTF_16BE)

		this.converter.write(utf16, contentType, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_16BE)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf(contentType.toString()))
		assertThat(result).isEqualTo("\"H\u00e9llo W\u00f6rld\"")
	}

	@Test
	fun canReadBigDecimalWithSerializerModule() {
		val customConverter = KotlinSerializationJsonHttpMessageConverter(customJson)
		assertThat(customConverter.canRead(BigDecimal::class.java, MediaType.APPLICATION_JSON)).isTrue()
	}

	@Test
	fun canWriteBigDecimalWithSerializerModule() {
		val customConverter = KotlinSerializationJsonHttpMessageConverter(customJson)
		assertThat(customConverter.canWrite(BigDecimal::class.java, MediaType.APPLICATION_JSON)).isTrue()
	}

	@Test
	fun readBigDecimalWithSerializerModule() {
		val body = "1.0"
		val inputMessage = MockHttpInputMessage(body.toByteArray(charset("UTF-8")))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val customConverter = KotlinSerializationJsonHttpMessageConverter(customJson)
		val result = customConverter.read(BigDecimal::class.java, inputMessage) as BigDecimal

		assertThat(result).isEqualTo(BigDecimal.valueOf(1.0))
	}

	@Test
	fun writeBigDecimalWithSerializerModule() {
		val outputMessage = MockHttpOutputMessage()
		val customConverter = KotlinSerializationJsonHttpMessageConverter(customJson)

		customConverter.write(BigDecimal(1), null, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/json"))
		assertThat(result).isEqualTo("1.0")
	}


	@Serializable
	@Suppress("ArrayInDataClass")
	data class SerializableBean(
			val bytes: ByteArray,
			val array: Array<String>,
			val number: Int,
			val string: String?,
			val bool: Boolean,
			val fraction: Float,
			val serializableBean: SerializableBean? = null
	)

	data class NotSerializableBean(val string: String)

	open class TypeBase<T>

	inline fun <reified T> typeTokenOf(): Type {
		val base = object : TypeBase<T>() {}
		val superType = base::class.java.genericSuperclass!!
		return (superType as ParameterizedType).actualTypeArguments.first()!!
	}

}
