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

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono

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

	class Foo
}
