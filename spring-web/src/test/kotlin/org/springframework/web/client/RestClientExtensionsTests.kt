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

package org.springframework.web.client

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference

/**
 * Mock object based tests for [RestClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
class RestClientExtensionsTests {

	private val requestBodySpec = mockk<RestClient.RequestBodySpec>(relaxed = true)

	private val responseSpec = mockk<RestClient.ResponseSpec>(relaxed = true)

	@Test
	fun `RequestBodySpec#body with reified type parameters`() {
		val body = mockk<List<Foo>>()
		requestBodySpec.bodyWithType(body)
		verify { requestBodySpec.body(body, object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#body with reified type parameters`() {
		responseSpec.body<List<Foo>>()
		verify { responseSpec.body(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	@Test
	fun `ResponseSpec#toEntity with reified type parameters`() {
		responseSpec.toEntity<List<Foo>>()
		verify { responseSpec.toEntity(object : ParameterizedTypeReference<List<Foo>>() {}) }
	}

	private class Foo

}
