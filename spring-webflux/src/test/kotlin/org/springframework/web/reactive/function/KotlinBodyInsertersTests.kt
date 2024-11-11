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

package org.springframework.web.reactive.function

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse
import reactor.test.StepVerifier
import java.util.*

/**
 * @author Sebastien Deleuze
 */
class KotlinBodyInsertersTests {

	private lateinit var context: BodyInserter.Context

	private lateinit var hints: Map<String, Any>


	@BeforeEach
	fun createContext() {
		val messageWriters: MutableList<HttpMessageWriter<*>> = ArrayList()
		val jsonEncoder = KotlinSerializationJsonEncoder()
		messageWriters.add(EncoderHttpMessageWriter(jsonEncoder))

		this.context = object : BodyInserter.Context {
			override fun messageWriters(): List<HttpMessageWriter<*>> {
				return messageWriters
			}

			override fun serverRequest(): Optional<ServerHttpRequest> {
				return Optional.empty()
			}

			override fun hints(): Map<String, Any> {
				return hints
			}
		}
		this.hints = HashMap()
	}

	@Test
	fun ofObjectWithBodyType() {
		val somebody = SomeBody(1, "name")
		val body = listOf(somebody)
		val inserter = BodyInserters.fromValue(body, object: ParameterizedTypeReference<List<SomeBody>>() {})
		val response = MockServerHttpResponse()
		val result = inserter.insert(response, context)
		StepVerifier.create(result).expectComplete().verify()

		StepVerifier.create(response.bodyAsString)
			.expectNext("[{\"user_id\":1,\"name\":\"name\"}]")
			.expectComplete()
			.verify()
	}

	@Serializable
	data class SomeBody(@SerialName("user_id") val userId: Int, val name: String)
}
