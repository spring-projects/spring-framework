/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.function.client

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import reactor.core.publisher.Mono

/**
 * Mock object based tests for [WebClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class WebClientExtensionsTests {

	private val requestBodySpec = mockk<WebClient.RequestBodySpec>(relaxed = true)

	private val responseSpec = mockk<WebClient.ResponseSpec>(relaxed = true)


	@Test
	fun `RequestBodySpec#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		requestBodySpec.body(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#bodyToMono with reified type parameters`() {
		responseSpec.bodyToMono<List<Foo>>()
		verify { responseSpec.bodyToMono(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#bodyToFlux with reified type parameters`() {
		responseSpec.bodyToFlux<List<Foo>>()
		verify { responseSpec.bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun awaitResponse() {
		val response = mockk<ClientResponse>()
		every { requestBodySpec.exchange() } returns Mono.just(response)
		runBlocking {
			assertEquals(response, requestBodySpec.awaitResponse())
		}
	}

	@Test
	fun body() {
		val headerSpec = mockk<WebClient.RequestHeadersSpec<*>>()
		val supplier: suspend () -> String = mockk()
		every { requestBodySpec.body(ofType<Mono<String>>()) } returns headerSpec
		runBlocking {
			requestBodySpec.body(supplier)
		}
		verify {
			requestBodySpec.body(ofType<Mono<String>>())
		}
	}

	@Test
	fun awaitBody() {
		val spec = mockk<WebClient.ResponseSpec>()
		every { spec.bodyToMono<String>() } returns Mono.just("foo")
		runBlocking {
			assertEquals("foo", spec.awaitBody<String>())
		}
	}

	class Foo
}
