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

package org.springframework.test.web.reactive.server

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.server.router
import java.util.concurrent.CompletableFuture

/**
 * Mock object based tests for [WebTestClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class WebTestClientExtensionsTests {

	private val requestBodySpec = mockk<WebTestClient.RequestBodySpec>(relaxed = true)

	private val responseSpec = mockk<WebTestClient.ResponseSpec>(relaxed = true)


	@Test
	fun `RequestBodySpec#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<Foo>>()
		requestBodySpec.body(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `RequestBodySpec#body with Flow and reified type parameters`() {
		val body = mockk<Flow<Foo>>()
		requestBodySpec.body(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `RequestBodySpec#body with CompletableFuture and reified type parameters`() {
		val body = mockk<CompletableFuture<Foo>>()
		requestBodySpec.body<Foo>(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `ResponseSpec#expectBody with reified type parameters`() {
		responseSpec.expectBody<Foo>()
		verify { responseSpec.expectBody(object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `KotlinBodySpec#isEqualTo`() {
		WebTestClient
				.bindToRouterFunction( router { GET("/") { ok().bodyValue("foo") } } )
				.build()
				.get().uri("/").exchange().expectBody<String>().isEqualTo("foo")
	}

	@Test
	fun `KotlinBodySpec#consumeWith`() {
		WebTestClient
				.bindToRouterFunction( router { GET("/") { ok().bodyValue("foo") } } )
				.build()
				.get().uri("/").exchange().expectBody<String>().consumeWith { assertThat(it.responseBody).isEqualTo("foo") }
	}

	@Test
	fun `KotlinBodySpec#returnResult`() {
		WebTestClient
				.bindToRouterFunction( router { GET("/") { ok().bodyValue("foo") } } )
				.build()
				.get().uri("/").exchange().expectBody<String>().returnResult().apply { assertThat(responseBody).isEqualTo("foo") }
	}

	@Test
	fun `ResponseSpec#expectBodyList with reified type parameters`() {
		responseSpec.expectBodyList<Foo>()
		verify { responseSpec.expectBodyList(object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `ResponseSpec#returnResult with reified type parameters`() {
		responseSpec.returnResult<Foo>()
		verify { responseSpec.returnResult(object : ParameterizedTypeReference<Foo>() {}) }
	}

	class Foo

}
