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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.Part
import org.springframework.util.CollectionUtils
import org.springframework.util.MultiValueMap
import org.springframework.web.server.WebSession
import reactor.core.publisher.Mono
import java.net.InetSocketAddress
import java.security.Principal
import java.util.*

/**
 * Mock object based tests for [ServerRequest] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 * @author Igor Manushin
 */
class ServerRequestExtensionsTests {

	val request = mockk<ServerRequest>(relaxed = true)

	val headers = mockk<ServerRequest.Headers>(relaxed = true)

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
	fun `bodyToFlow with KClass parameters`() {
		request.bodyToFlow(String::class)
		verify { request.bodyToFlux(String::class.java) }
	}

	@Test
	fun `awaitBody with reified type parameters`() {
		every { request.bodyToMono<String>() } returns Mono.just("foo")
		runBlocking {
			assertThat(request.awaitBody<String>()).isEqualTo("foo")
		}
	}

	@Test
	fun `awaitBody with KClass parameters`() {
		every { request.bodyToMono(String::class.java) } returns Mono.just("foo")
		runBlocking {
			assertThat(request.awaitBody(String::class)).isEqualTo("foo")
		}
	}

	@Test
	fun `awaitBodyOrNull with reified type parameters`() {
		every { request.bodyToMono<String>() } returns Mono.empty()
		runBlocking {
			assertThat(request.awaitBodyOrNull<String>()).isNull()
		}
	}

	@Test
	fun `awaitBodyOrNull with KClass parameters`() {
		every { request.bodyToMono(String::class.java) } returns Mono.empty()
		runBlocking {
			assertThat(request.awaitBodyOrNull(String::class)).isNull()
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

	@Test
	fun `remoteAddressOrNull with value`() {
		val remoteAddress = InetSocketAddress(1234)
		every { request.remoteAddress() } returns Optional.of(remoteAddress)
		assertThat(remoteAddress).isEqualTo(request.remoteAddressOrNull())
		verify { request.remoteAddress() }
	}

	@Test
	fun `remoteAddressOrNull with null`() {
		every { request.remoteAddress() } returns Optional.empty()
		assertThat(request.remoteAddressOrNull()).isNull()
		verify { request.remoteAddress() }
	}

	@Test
	fun `attributeOrNull with value`() {
		every { request.attributes() } returns mapOf("foo" to "bar")
		assertThat(request.attributeOrNull("foo")).isEqualTo("bar")
		verify { request.attributes() }
	}

	@Test
	fun `attributeOrNull with null`() {
		every { request.attributes() } returns mapOf("foo" to "bar")
		assertThat(request.attributeOrNull("baz")).isNull()
		verify { request.attributes() }
	}

	@Test
	fun `queryParamOrNull with value`() {
		every { request.queryParams() } returns CollectionUtils.toMultiValueMap(mapOf("foo" to listOf("bar")))
		assertThat(request.queryParamOrNull("foo")).isEqualTo("bar")
		verify { request.queryParams() }
	}

	@Test
	fun `queryParamOrNull with values`() {
		every { request.queryParams() } returns CollectionUtils.toMultiValueMap(mapOf("foo" to listOf("bar", "bar")))
		assertThat(request.queryParamOrNull("foo")).isEqualTo("bar")
		verify { request.queryParams() }
	}

	@Test
	fun `queryParamOrNull with null value`() {
		every { request.queryParams() } returns CollectionUtils.toMultiValueMap(mapOf("foo" to listOf(null)))
		assertThat(request.queryParamOrNull("foo")).isEqualTo("")
		verify { request.queryParams() }
	}

	@Test
	fun `queryParamOrNull with null`() {
		every { request.queryParams() } returns CollectionUtils.toMultiValueMap(mapOf("foo" to listOf("bar")))
		assertThat(request.queryParamOrNull("baz")).isNull()
		verify { request.queryParams() }
	}

	@Test
	fun `contentLengthOrNull with value`() {
		every { headers.contentLength() } returns OptionalLong.of(123)
		assertThat(headers.contentLengthOrNull()).isEqualTo(123)
		verify { headers.contentLength() }
	}

	@Test
	fun `contentLengthOrNull with null`() {
		every { headers.contentLength() } returns OptionalLong.empty()
		assertThat(headers.contentLengthOrNull()).isNull()
		verify { headers.contentLength() }
	}

	@Test
	fun `contentTypeOrNull with value`() {
		every { headers.contentType() } returns Optional.of(MediaType.APPLICATION_JSON)
		assertThat(headers.contentTypeOrNull()).isEqualTo(MediaType.APPLICATION_JSON)
		verify { headers.contentType() }
	}

	@Test
	fun `contentTypeOrNull with null`() {
		every { headers.contentType() } returns Optional.empty()
		assertThat(headers.contentTypeOrNull()).isNull()
		verify { headers.contentType() }
	}

	class Foo
}
