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

package org.springframework.web.servlet.function

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import java.net.InetSocketAddress
import java.security.Principal
import java.util.*

/**
 * Tests for WebMvc.fn [ServerRequest] extensions.
 *
 * @author Sebastien Deleuze
 */
class ServerRequestExtensionsTests {

	val request = mockk<ServerRequest>()

	val headers = mockk<ServerRequest.Headers>()

	@Test
	fun `remoteAddressOrNull with value`() {
		val remoteAddress = mockk<InetSocketAddress>()
		every { request.remoteAddress() } returns Optional.of(remoteAddress)
		assertEquals(remoteAddress, request.remoteAddressOrNull())
		verify { request.remoteAddress() }
	}

	@Test
	fun `remoteAddressOrNull with null`() {
		every { request.remoteAddress() } returns Optional.empty()
		assertNull(request.remoteAddressOrNull())
		verify { request.remoteAddress() }
	}

	@Test
	fun body() {
		val body = Arrays.asList("foo", "bar")
		val typeReference = object: ParameterizedTypeReference<List<String>>() {}
		every { request.body(typeReference) } returns body
		assertEquals(body, request.body<List<String>>())
		verify { request.body(typeReference) }
	}

	@Test
	fun `attributeOrNull with value`() {
		val attribute = mockk<Any>()
		every { request.attribute("foo") } returns Optional.of(attribute)
		assertEquals(attribute, request.attributeOrNull("foo"))
		verify { request.attribute("foo") }
	}

	@Test
	fun `attributeOrNull with null`() {
		every { request.attribute("foo") } returns Optional.empty()
		assertNull(request.attributeOrNull("foo"))
		verify { request.attribute("foo") }
	}

	@Test
	fun `paramOrNull with value`() {
		val param = "bar"
		every { request.param("foo") } returns Optional.of(param)
		assertEquals(param, request.paramOrNull("foo"))
		verify { request.param("foo") }
	}

	@Test
	fun `paramOrNull with null`() {
		every { request.param("foo") } returns Optional.empty()
		assertNull(request.paramOrNull("foo"))
		verify { request.param("foo") }
	}

	@Test
	fun `principalOrNull with value`() {
		val principal = mockk<Principal>()
		every { request.principal() } returns Optional.of(principal)
		assertEquals(principal, request.principalOrNull())
		verify { request.principal() }
	}

	@Test
	fun `principalOrNull with null`() {
		every { request.principal() } returns Optional.empty()
		assertNull(request.principalOrNull())
		verify { request.principal() }
	}

	@Test
	fun `contentLengthOrNull with value`() {
		val contentLength: Long = 123
		every { headers.contentLength() } returns OptionalLong.of(contentLength)
		assertEquals(contentLength, headers.contentLengthOrNull())
		verify { headers.contentLength() }
	}

	@Test
	fun `contentLengthOrNull with null`() {
		every { headers.contentLength() } returns OptionalLong.empty()
		assertNull(headers.contentLengthOrNull())
		verify { headers.contentLength() }
	}

	@Test
	fun `contentTypeOrNull with value`() {
		val contentType = mockk<MediaType>()
		every { headers.contentType() } returns Optional.of(contentType)
		assertEquals(contentType, headers.contentTypeOrNull())
		verify { headers.contentType() }
	}

	@Test
	fun `contentTypeOrNull with null`() {
		every { headers.contentType() } returns Optional.empty()
		assertNull(headers.contentTypeOrNull())
		verify { headers.contentType() }
	}
}
