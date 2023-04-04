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

package org.springframework.web.reactive.result.method.annotation

import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.EncoderHttpMessageWriter
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.util.ObjectUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest
import org.springframework.web.testfixture.method.ResolvableMethod
import org.springframework.web.testfixture.server.MockServerWebExchange
import reactor.test.StepVerifier
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.*

/**
 * Kotlin unit tests for {@link AbstractMessageWriterResultHandler}.
 *
 * @author Sebastien Deleuze
 */
class KotlinMessageWriterResultHandlerTests {

	private val resultHandler = initResultHandler()

	private val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"))


	private fun initResultHandler(vararg writers: HttpMessageWriter<*>): AbstractMessageWriterResultHandler {
		val writerList = if (ObjectUtils.isEmpty(writers)) {
			listOf(
				EncoderHttpMessageWriter(KotlinSerializationJsonEncoder()),
				EncoderHttpMessageWriter(Jackson2JsonEncoder())
			)
		} else {
			listOf(*writers)
		}
		val resolver = RequestedContentTypeResolverBuilder().build()
		return object : AbstractMessageWriterResultHandler(writerList, resolver) {}
	}

	@Test
	fun nonSuspendWithoutResponseEntity() {
		val returnType = ResolvableMethod.on(SampleController::class.java)
			.resolveReturnType(List::class.java, Person::class.java)
		val body = listOf(Person(UserId(1), "John"))
		resultHandler.writeBody(body, returnType, exchange).block(Duration.ofSeconds(5))

		Assertions.assertThat(exchange.response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
		assertResponseBody("[{\"userId\":1,\"name\":\"John\"}]")
	}

	@Test
	fun nonSuspendWithResponseEntity() {
		val returnType = ResolvableMethod.on(SampleController::class.java)
			.returning(ResolvableType.forClassWithGenerics(ResponseEntity::class.java,
				ResolvableType.forClassWithGenerics(List::class.java, Person::class.java)))
			.build().returnType()
		val body = ResponseEntity.ok(listOf(Person(UserId(1), "John")))
		resultHandler.writeBody(body.body, returnType.nested(), returnType, exchange).block(Duration.ofSeconds(5))

		Assertions.assertThat(exchange.response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
		assertResponseBody("[{\"userId\":1,\"name\":\"John\"}]")
	}

	@Test
	fun suspendWithoutResponseEntity() {
		val returnType = ResolvableMethod.on(CoroutinesSampleController::class.java)
			.resolveReturnType(List::class.java, Person::class.java)
		val body = listOf(Person(UserId(1), "John"))
		resultHandler.writeBody(body, returnType, exchange).block(Duration.ofSeconds(5))

		Assertions.assertThat(exchange.response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
		assertResponseBody("[{\"userId\":1,\"name\":\"John\"}]")
	}

	@Test
	fun suspendWithResponseEntity() {
		val returnType = ResolvableMethod.on(CoroutinesSampleController::class.java)
			.returning(ResolvableType.forClassWithGenerics(ResponseEntity::class.java,
				ResolvableType.forClassWithGenerics(List::class.java, Person::class.java)))
			.build().returnType()
		val body = ResponseEntity.ok(listOf(Person(UserId(1), "John")))
		resultHandler.writeBody(body.body, returnType.nested(), returnType, exchange).block(Duration.ofSeconds(5))

		Assertions.assertThat(exchange.response.headers.contentType).isEqualTo(MediaType.APPLICATION_JSON)
		assertResponseBody("[{\"userId\":1,\"name\":\"John\"}]")
	}

	private fun assertResponseBody(responseBody: String) {
		StepVerifier.create(exchange.response.body)
			.consumeNextWith { buf: DataBuffer ->
				Assertions.assertThat(
					buf.toString(StandardCharsets.UTF_8)
				).isEqualTo(responseBody)
			}
			.expectComplete()
			.verify()
	}

	@RestController
	class SampleController {

		@GetMapping("/non-suspend-with-response-entity")
		fun withResponseEntity(): ResponseEntity<List<Person>> =
			TODO()

		@GetMapping("/non-suspend-without-response-entity")
		fun withoutResponseEntity(): List<Person> =
			TODO()
	}

	@RestController
	class CoroutinesSampleController {

		@GetMapping("/suspend-with-response-entity")
		suspend fun suspendAndResponseEntity(): ResponseEntity<List<Person>> =
			TODO()

		@GetMapping("/suspend-without-response-entity")
		suspend fun suspendWithoutResponseEntity(): List<Person> =
			TODO()

	}

	@Serializable
	data class Person(
		val userId: UserId,
		val name: String,
	)

	@JvmInline
	@Serializable
	value class UserId(val id: Int)

}
