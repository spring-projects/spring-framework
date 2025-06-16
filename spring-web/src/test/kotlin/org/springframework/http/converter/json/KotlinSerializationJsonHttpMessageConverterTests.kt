/*
 * Copyright 2002-2025 the original author or authors.
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.customJson
import org.springframework.web.testfixture.http.MockHttpInputMessage
import org.springframework.web.testfixture.http.MockHttpOutputMessage
import java.lang.reflect.ParameterizedType
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import kotlin.reflect.KType
import kotlin.reflect.javaType
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

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

		assertThat(converter.canRead(Map::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<Map<String, SerializableBean>>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<List<SerializableBean>>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(Set::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<Set<SerializableBean>>(), MediaType.APPLICATION_JSON)).isTrue()

		assertThat(converter.canRead(resolvableTypeOf<List<Int>>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<ArrayList<Int>>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<List<Int>>(), MediaType.APPLICATION_PDF)).isFalse()

		assertThat(converter.canRead(resolvableTypeOf<Ordered>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<List<Ordered>>(), MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<OrderedImpl>(), MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(resolvableTypeOf<List<OrderedImpl>>(), MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(ResolvableType.forType(ResolvableType.NONE.type), MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(ResolvableType.forType(BigDecimal::class.java), MediaType.APPLICATION_JSON)).isFalse()
	}

	@Test
	fun canWriteJson() {
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_PDF)).isFalse()
		assertThat(converter.canWrite(String::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(NotSerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(Map::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<Map<String, SerializableBean>>(), Map::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<List<SerializableBean>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(Set::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<Set<SerializableBean>>(), Set::class.java, MediaType.APPLICATION_JSON)).isTrue()

		assertThat(converter.canWrite(resolvableTypeOf<List<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<ArrayList<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<List<Int>>(), List::class.java, MediaType.APPLICATION_PDF)).isFalse()

		assertThat(converter.canWrite(resolvableTypeOf<Ordered>(), Ordered::class.java, MediaType.APPLICATION_JSON)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<OrderedImpl>(), OrderedImpl::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(ResolvableType.NONE, SerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(ResolvableType.forType(BigDecimal::class.java), BigDecimal::class.java, MediaType.APPLICATION_JSON)).isFalse()
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
		val result = converter.read(ResolvableType.forType(typeOf<List<SerializableBean>>().javaType), inputMessage, null)
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
	@Suppress("UNCHECKED_CAST")
	fun readNullableWithNull() {
		val body = """{"value":null}"""

		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_8))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val methodParameter = MethodParameter.forExecutable(::handleMapWithNullable::javaMethod.get()!!, 0)
		val hints = mapOf(KType::class.jvmName to typeOf<Map<String, String?>>())

		val result = converter.read(ResolvableType.forMethodParameter(methodParameter), inputMessage,
			hints) as Map<String, String?>

		assertThat(result).containsExactlyEntriesOf(mapOf("value" to null))
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun readWithPolymorphism() {
		val json = Json {
			serializersModule = SerializersModule {
				polymorphic(ISimpleSerializableBean::class, SimpleSerializableBean::class, SimpleSerializableBean.serializer())
			}
		}
		val customConverter = KotlinSerializationJsonHttpMessageConverter(json)
		val body = """[{"type":"org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverterTests.SimpleSerializableBean","name":"foo"},""" +
				"""{"type":"org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverterTests.SimpleSerializableBean","name":"bar"}]"""

		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_8))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		val resolvableType = ResolvableType.forClassWithGenerics(List::class.java, ISimpleSerializableBean::class.java)
		val result = customConverter.read(resolvableType, inputMessage, null) as List<ISimpleSerializableBean>

		assertThat(result).containsExactly(SimpleSerializableBean("foo"), SimpleSerializableBean("bar"))
	}

	@Test
	fun writeObject() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)

		this.converter.write(serializableBean, null, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
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

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
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

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
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

		this.converter.write(arrayListOf(serializableBean), ResolvableType.forType(typeOf<List<SerializableBean>>().javaType), null,
				outputMessage, null)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
		assertThat(result).isEqualTo(expectedJson)
	}

	@Test
	fun writeObjectInUtf16() {
		val outputMessage = MockHttpOutputMessage()
		val utf16 = "H\u00e9llo W\u00f6rld"
		val contentType = MediaType("application", "json", StandardCharsets.UTF_16BE)

		this.converter.write(utf16, contentType, outputMessage)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_16BE)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", contentType.toString())).isTrue()
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

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
		assertThat(result).isEqualTo("1.0")
	}

	@Test
	@ExperimentalStdlibApi
	fun writeNullableWithNull() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = mapOf<String, String?>("value" to null)
		val expectedJson = """{"value":null}"""
		val methodParameter = MethodParameter.forExecutable(::handleMapWithNullable::javaMethod.get()!!, -1)
		val hints = mapOf(KType::class.jvmName to typeOf<Map<String, String?>>())

		this.converter.write(serializableBean, ResolvableType.forMethodParameter(methodParameter), null,
			outputMessage, hints)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
		assertThat(result).isEqualTo(expectedJson)
	}

	@Test
	fun writeProperty() {
		val outputMessage = MockHttpOutputMessage()
		val method = this::class.java.getDeclaredMethod("getValue")
		val methodParameter = MethodParameter.forExecutable(method, -1)

		this.converter.write(value, ResolvableType.forMethodParameter(methodParameter), null, outputMessage, null)
		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
		assertThat(result).isEqualTo("42")
	}

	@Test
	fun writeWithPolymorphism() {
		val json = Json {
			serializersModule = SerializersModule {
				polymorphic(ISimpleSerializableBean::class, SimpleSerializableBean::class, SimpleSerializableBean.serializer())
			}
		}
		val customConverter = KotlinSerializationJsonHttpMessageConverter(json)

		val outputMessage = MockHttpOutputMessage()
		val value = listOf(SimpleSerializableBean("foo"), SimpleSerializableBean("bar"))
		customConverter.write(value, ResolvableType.forClassWithGenerics(List::class.java, ISimpleSerializableBean::class.java), null, outputMessage, null)

		val result = outputMessage.getBodyAsString(StandardCharsets.UTF_8)

		assertThat(outputMessage.headers.containsHeaderValue("Content-Type", "application/json")).isTrue()
		assertThat(result).isEqualTo(
			"""[{"type":"org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverterTests.SimpleSerializableBean","name":"foo"},""" +
					"""{"type":"org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverterTests.SimpleSerializableBean","name":"bar"}]""")
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

	private inline fun <reified T> resolvableTypeOf(): ResolvableType {
		val base = object : TypeBase<T>() {}
		val superType = base::class.java.genericSuperclass!!
		return ResolvableType.forType((superType as ParameterizedType).actualTypeArguments.first()!!)
	}

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
