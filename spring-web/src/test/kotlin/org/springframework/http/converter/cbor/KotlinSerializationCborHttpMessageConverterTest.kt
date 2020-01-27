/*
 * Copyright 2002-2020 the original author or authors.
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

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.MockHttpInputMessage
import org.springframework.http.MockHttpOutputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import java.nio.charset.StandardCharsets

/**
 * Tests for the CBOR conversion using kotlinx.serialization.
 *
 * @author Andreas Ahlenstorf
 */
class KotlinSerializationCborHttpMessageConverterTest {

	private val converter = KotlinSerializationCborHttpMessageConverter()

	@Test
	internal fun canReadCbor() {
		assertThat(converter.canRead(SerializableBean::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canRead(String::class.java, MediaType.APPLICATION_CBOR)).isTrue()
	}

	@Test
	fun canWriteCbor() {
		assertThat(converter.canWrite(SerializableBean::class.java, MediaType.APPLICATION_CBOR)).isTrue()
		assertThat(converter.canWrite(String::class.java, MediaType.APPLICATION_CBOR)).isTrue()
	}

	@Test
	fun writeAndReadObject() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)

		this.converter.write(serializableBean, null, outputMessage)

		assertThat(outputMessage.headers.contentType).isEqualTo(MediaType.APPLICATION_CBOR)

		val inputMessage = MockHttpInputMessage(outputMessage.bodyAsBytes)

		val readBean = this.converter.read(SerializableBean::class.java, inputMessage) as SerializableBean

		assertThat(readBean.bytes).containsExactly(0x1, 0x2)
		assertThat(readBean.array).containsExactly("Foo", "Bar")
		assertThat(readBean.number).isEqualTo(42)
		assertThat(readBean.string).isEqualTo("Foo")
		assertThat(readBean.bool).isTrue()
		assertThat(readBean.fraction).isEqualTo(42.0f)
	}

	@Test
	@Suppress("UNCHECKED_CAST")
	fun writeAndReadArrayOfObjects() {
		val outputMessage = MockHttpOutputMessage()
		val serializableBean = SerializableBean(byteArrayOf(0x1, 0x2), arrayOf("Foo", "Bar"), 42, "Foo", true, 42.0f)

		this.converter.write(arrayOf(serializableBean), null, outputMessage)

		assertThat(outputMessage.headers.contentType).isEqualTo(MediaType.APPLICATION_CBOR)

		val inputMessage = MockHttpInputMessage(outputMessage.bodyAsBytes)

		val result = this.converter.read(Array<SerializableBean>::class.java, inputMessage) as Array<SerializableBean>

		assertThat(result).hasSize(1)
		assertThat(result[0].bytes).containsExactly(0x1, 0x2)
		assertThat(result[0].array).containsExactly("Foo", "Bar")
		assertThat(result[0].number).isEqualTo(42)
		assertThat(result[0].string).isEqualTo("Foo")
		assertThat(result[0].bool).isTrue()
		assertThat(result[0].fraction).isEqualTo(42.0f)
	}

	@Test
	internal fun readFailsOnInvalidCbor() {
		val body = """
			this is an invalid Cbor document
		""".trimIndent()

		val inputMessage = MockHttpInputMessage(body.toByteArray(StandardCharsets.UTF_8))
		inputMessage.headers.contentType = MediaType.APPLICATION_JSON
		assertThatExceptionOfType(HttpMessageNotReadableException::class.java).isThrownBy {
			converter.read(SerializableBean::class.java, inputMessage)
		}
	}

	@Serializable
	@Suppress("ArrayInDataClass")
	data class SerializableBean(
			val bytes: ByteArray,
			val array: Array<String>,
			val number: Int,
			val string: String,
			val bool: Boolean,
			val fraction: Float
	)
}