/*
 * Copyright 2002-2019 the original author or authors.
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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.security.Principal

/**
 * Mock object based tests for [ServerRequest] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
class ServerRequestExtensionsTests {

	val request = mockk<ServerRequest>(relaxed = true)

	@Test
	fun `bodyToMono with reified type parameters`() {
		request.bodyToMono<List<Foo>>()
		verify { request.bodyToMono(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `bodyToFlux with reified type parameters`() {
		request.bodyToFlux<List<Foo>>()
		verify { request.bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `bodyToFlow with reified type parameters`() {
		request.bodyToFlow<List<Foo>>()
		verify { request.bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun awaitBody() {
		every { request.bodyToMono<String>() } returns Mono.just("foo")
		runBlocking {
			assertThat(request.awaitBody<String>()).isEqualTo("foo")
		}
	}

	@Test
	fun awaitBodyOrNull() {
		every { request.bodyToMono<String>() } returns Mono.empty()
		runBlocking {
			assertThat(request.awaitBodyOrNull<String>()).isNull()
		}
	}

	@Test
	fun awaitFormData() {
		val map = mockk<MultiValueMap<String, String>>()
		every { request.formData() } returns Mono.just(map)
		runBlocking {
			assertThat(request.awaitFormData()).isEqualTo(map)
		}
	}

	@Test
	fun awaitMultipartData() {
		val map = mockk<MultiValueMap<String, Part>>()
		every { request.multipartData() } returns Mono.just(map)
		runBlocking {
			assertThat(request.awaitMultipartData()).isEqualTo(map)
		}
	}

	@Test
	fun awaitPrincipal() {
		val principal = mockk<Principal>()
		every { request.principal() } returns Mono.just(principal)
		runBlocking {
			assertThat(request.awaitPrincipal()).isEqualTo(principal)
		}
	}

	@Test
	fun awaitSession() {
		val session = mockk<WebSession>()
		every { request.session() } returns Mono.just(session)
		runBlocking {
			assertThat(request.awaitSession()).isEqualTo(session)
		}
	}


	class Foo
}
