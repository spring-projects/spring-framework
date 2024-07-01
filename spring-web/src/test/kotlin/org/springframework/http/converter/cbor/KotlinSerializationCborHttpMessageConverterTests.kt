/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.converter.cbor

import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets

import kotlin.reflect.javaType
import kotlin.reflect.typeOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

import org.springframework.core.Ordered
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.testfixture.http.MockHttpInputMessage
import org.springframework.web.testfixture.http.MockHttpOutputMessage

/**
 * Tests for the CBOR conversion using kotlinx.serialization.
 *
 * @author Andreas Ahlenstorf
 * @author Sebastien Deleuze
 * @author Iain Henderson
 */
@Suppress("UsePropertyAccessSyntax")
@ExperimentalSerializationApi
class KotlinSerializationCborHttpMessageConverterTests {

	private val converter = KotlinSerializationCborHttpMessageConverter()
	private val serializableBean = SerializableBean(
			bytes = byteArrayOf(0x1, 0x2),
			array = arrayOf("Foo", "Bar"),
			number = 42,
			string = "Foo",
			bool = true,
			fraction = 42f
	)
	private val serializableBeanArray = arrayOf(serializableBean)
	private val serializableBeanArrayBody = Cbor.Default.encodeToByteArray(serializableBeanArray)

	@Test
	fun canReadCbor() {
		assertThat(converter.canRead(SerializableBean::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(SerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canRead(String::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(NotSerializableBean::class.java, MediaType.APPLICATION_CBOR)).isFalse()

		assertThat(converter.canRead(Map::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canRead(resolvableTypeOf<Map<String, SerializableBean>>(), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(List::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canRead(resolvableTypeOf<List<SerializableBean>>(), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(Set::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canRead(resolvableTypeOf<Set<SerializableBean>>(), MediaType.APPLICATION_CBOR)).isTrue()

		assertThat(converter.canRead(resolvableTypeOf<List<Int>>(), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<ArrayList<Int>>(), MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(resolvableTypeOf<List<Int>>(), MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canRead(resolvableTypeOf<Ordered>(), MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canRead(resolvableTypeOf<List<Ordered>>(), MediaType.APPLICATION_CBOR)).isFalse()
	}

	@Test
	fun canWriteCbor() {
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_JSON)).isFalse()
		assertThat(converter.canWrite(String::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(NotSerializableBean::class.java, MediaType.APPLICATION_CBOR)).isFalse()

		assertThat(converter.canWrite(Map::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canWrite(resolvableTypeOf<Map<String, SerializableBean>>(), Map::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(List::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canWrite(resolvableTypeOf<List<SerializableBean>>(), List::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(Set::class.java, MediaType.APPLICATION_CBOR)).isFalse()
		assertThat(converter.canWrite(resolvableTypeOf<Set<SerializableBean>>(), Set::class.java, MediaType.APPLICATION_CBOR)).isTrue()

		assertThat(converter.canWrite(resolvableTypeOf<List<Int>>(), List::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<ArrayList<Int>>(), List::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(resolvableTypeOf<List<Int>>(), List::class.java, MediaType.APPLICATION_JSON)).isFalse()

		assertThat(converter.canWrite(resolvableTypeOf<Ordered>(), Ordered::class.java, MediaType.APPLICATION_CBOR)).isFalse()
	}

	@Test
	fun readObject() {
		val serializableBeanBody = Cbor.Default.encodeToByteArray(serializableBean)
		val inputMessage = MockHttpInputMessage(serializableBeanBody)
		inputMessage.headers.contentType = MediaType.APPLICATION_CBOR
		val result = converter.read(SerializableBean::class.java, inputMessage) as SerializableBean

		assertThat(result.bytes).containsExactly(*serializableBean.bytes)
		assertThat(result.array).containsExactly(*serializableBean.array)
		assertThat(result.number).isEqualTo(serializableBean.number)
		assertThat(result.string).isEqualTo(serializableBean.string)
		assertThat(result.bool).isTrue()
		assertThat(result.fraction).isEqualTo(serializableBean.fraction)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun readArrayOfObjects() {
		val inputMessage = MockHttpInputMessage(serializableBeanArrayBody)
		inputMessage.headers.contentType = MediaType.APPLICATION_CBOR
		val result = converter.read(Array<SerializableBean>::class.java, inputMessage) as Array<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(*serializableBean.bytes)
		assertThat(result[0].array).containsExactly(*serializableBean.array)
		assertThat(result[0].number).isEqualTo(serializableBean.number)
		assertThat(result[0].string).isEqualTo(serializableBean.string)
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(serializableBean.fraction)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	@ExperimentalStdlibApi
	fun readGenericCollection() {
		val inputMessage = MockHttpInputMessage(serializableBeanArrayBody)
		inputMessage.headers.contentType = MediaType.APPLICATION_CBOR
		val result = converter.read(ResolvableType.forType(typeOf<List<SerializableBean>>().javaType), inputMessage, null)
				as List<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(*serializableBean.bytes)
		assertThat(result[0].array).containsExactly(*serializableBean.array)
		assertThat(result[0].number).isEqualTo(serializableBean.number)
		assertThat(result[0].string).isEqualTo(serializableBean.string)
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(serializableBean.fraction)
	}

	@Test
	fun readFailsOnInvalidCbor() {
		val body = """
			this is an invalid JSON document and definitely NOT CBOR
		""".trimIndent()

		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_8))
		inputMessage.headers.contentType = MediaType.APPLICATION_CBOR
		assertThatExceptionOfType(HttpMessageNotReadableException::class.java).isThrownBy {
			converter.read(SerializableBean::class.java, inputMessage)
		}
	}

	@Test
	fun writeObject() {
		val outputMessage = MockHttpOutputMessage()

		this.converter.write(serializableBean, null, outputMessage)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/cbor"))
		assertThat(outputMessage.bodyAsBytes.isNotEmpty()).isTrue()
	}

	@Test
	fun writeObjectWithNullableProperty() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, null, true, 42.0f)

		this.converter.write(serializableBean, null, outputMessage)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/cbor"))
		assertThat(outputMessage.bodyAsBytes.isNotEmpty()).isTrue()
	}

	@Test
	fun writeArrayOfObjects() {
		val outputMessage = MockHttpOutputMessage()

		this.converter.write(serializableBeanArray, null, outputMessage)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/cbor"))
		assertThat(outputMessage.bodyAsBytes.isNotEmpty()).isTrue()
	}

	@Test
	@ExperimentalStdlibApi
	fun writeGenericCollection() {
		val outputMessage = MockHttpOutputMessage()

		this.converter.write(listOf(serializableBean), ResolvableType.forType(typeOf<List<SerializableBean>>().javaType), null, outputMessage, null)

		assertThat(outputMessage.headers).containsEntry("Content-Type", listOf("application/cbor"))
		assertThat(outputMessage.bodyAsBytes.isNotEmpty()).isTrue()
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

}
