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

package org.springframework.web.reactive.function.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.*
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture

/**
 * Mock object based tests for [ServerResponse] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@Suppress("UnassignedFluxMonoInstance")
class ServerResponseExtensionsTests {

	private val bodyBuilder = mockk<ServerResponse.BodyBuilder>(relaxed = true)


	@Test
	fun `BodyBuilder#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		bodyBuilder.body(body)
		verify { bodyBuilder.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `BodyBuilder#body with CompletableFuture and reified type parameters`() {
		val body = mockk<CompletableFuture<List<Foo>>>()
		bodyBuilder.body<List<Foo>>(body)
		verify { bodyBuilder.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `BodyBuilder#body with Flowable and reified type parameters`() {
		val body = mockk<Flowable<List<Foo>>>()
		bodyBuilder.body(body)
		verify { bodyBuilder.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `BodyBuilder#bodyAndAwait with object parameter`() {
		val response = mockk<ServerResponse>()
		val body = "foo"
		every { bodyBuilder.bodyValue(ofType<String>()) } returns Mono.just(response)
		runBlocking {
			bodyBuilder.bodyValueAndAwait(body)
		}
		verify {
			bodyBuilder.bodyValue(ofType<String>())
		}
	}

	@Test
	fun `BodyBuilder#bodyAndAwait with flow parameter`() {
		val response = mockk<ServerResponse>()
		val body = mockk<Flow<List<Foo>>>()
		every { bodyBuilder.body(ofType<Flow<List<Foo>>>(), object : ParameterizedTypeReference<List<Foo>>() {}) } returns Mono.just(response)
		runBlocking {
			bodyBuilder.bodyAndAwait(body)
		}
		verify {
			bodyBuilder.body(ofType<Flow<List<Foo>>>(), object : ParameterizedTypeReference<List<Foo>>() {})
		}
	}

	@Test
	fun `BodyBuilder#json`() {
		bodyBuilder.json()
		verify { bodyBuilder.contentType(APPLICATION_JSON) }
	}

	@Test
	fun `BodyBuilder#xml`() {
		bodyBuilder.xml()
		verify { bodyBuilder.contentType(APPLICATION_XML) }
	}

	@Test
	fun `BodyBuilder#html`() {
		bodyBuilder.html()
		verify { bodyBuilder.contentType(TEXT_HTML) }
	}

	@Test
	fun `BodyBuilder#sse`() {
		bodyBuilder.sse()
		verify { bodyBuilder.contentType(TEXT_EVENT_STREAM) }
	}

	@Test
	fun `BodyBuilder#renderAndAwait with a vararg parameter`() {
		val response = mockk<ServerResponse>()
		every { bodyBuilder.render("foo", any(), any()) } returns Mono.just(response)
		runBlocking {
			bodyBuilder.renderAndAwait("foo", "bar", "baz")
		}
		verify {
			bodyBuilder.render("foo", any(), any())
		}
	}

	@Test
	fun `BodyBuilder#renderAndAwait with a Map parameter`() {
		val response = mockk<ServerResponse>()
		val map = mockk<Map<String, *>>()
		every { bodyBuilder.render("foo", map) } returns Mono.just(response)
		runBlocking {
			bodyBuilder.renderAndAwait("foo", map)
		}
		verify {
			bodyBuilder.render("foo", map)
		}
	}

	@Test
	fun `HeadersBuilder#buildAndAwait`() {
		val response = mockk<ServerResponse>()
		val builder = mockk<ServerResponse.HeadersBuilder<*>>()
		every { builder.build() } returns Mono.just(response)
		runBlocking {
			assertThat(builder.buildAndAwait()).isEqualTo(response)
		}
	}

	class Foo
}
