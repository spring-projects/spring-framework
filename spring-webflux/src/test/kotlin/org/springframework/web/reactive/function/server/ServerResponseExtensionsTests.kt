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

package org.springframework.web.reactive.function.server

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.reactivestreams.Publisher
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType.*
import reactor.core.publisher.Mono

/**
 * Mock object based tests for [ServerResponse] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class ServerResponseExtensionsTests {

	private val bodyBuilder = mockk<ServerResponse.BodyBuilder>(relaxed = true)


	@Test
	fun `BodyBuilder#body with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		bodyBuilder.body(body)
		verify { bodyBuilder.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `BodyBuilder#bodyToServerSentEvents with Publisher and reified type parameters`() {
		val body = mockk<Publisher<List<Foo>>>()
		bodyBuilder.bodyToServerSentEvents(body)
		verify { bodyBuilder.contentType(TEXT_EVENT_STREAM) }
	}

	@Test
	fun `BodyBuilder#json`() {
		bodyBuilder.json()
		verify { bodyBuilder.contentType(APPLICATION_JSON_UTF8) }
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
	fun await() {
		val response = mockk<ServerResponse>()
		val builder = mockk<ServerResponse.HeadersBuilder<*>>()
		every { builder.build() } returns Mono.just(response)
		runBlocking {
			assertEquals(response, builder.buildAndAwait())
		}
	}

	@Test
	fun `bodyAndAwait with object parameter`() {
		val response = mockk<ServerResponse>()
		val body = "foo"
		every { bodyBuilder.syncBody(ofType<String>()) } returns Mono.just(response)
		runBlocking {
			bodyBuilder.bodyAndAwait(body)
		}
		verify {
			bodyBuilder.syncBody(ofType<String>())
		}
	}

	@Test
	fun `renderAndAwait with a vararg parameter`() {
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
	fun `renderAndAwait with a Map parameter`() {
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

	class Foo
}
