/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.client

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.core.ParameterizedTypeReference

/**
 * Mock object based tests for [ClientResponse] Kotlin extensions.
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class ClientResponseExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var response: ClientResponse

	@Test
	fun `bodyToMono with reified type parameters`() {
		response.bodyToMono<List<Foo>>()
		verify(response, times(1)).bodyToMono(object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `bodyToFlux with reified type parameters`() {
		response.bodyToFlux<List<Foo>>()
		verify(response, times(1)).bodyToFlux(object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `toEntity with reified type parameters`() {
		response.toEntity<List<Foo>>()
		verify(response, times(1)).toEntity(object : ParameterizedTypeReference<List<Foo>>() {})
	}

	@Test
	fun `ResponseSpec#toEntityList with reified type parameters`() {
		response.toEntityList<List<Foo>>()
		verify(response, times(1)).toEntityList(object : ParameterizedTypeReference<List<Foo>>() {})
	}

	class Foo
}
