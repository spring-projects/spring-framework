/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.test.web.reactive.server

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.reactivestreams.Publisher

/**
 * Mock object based tests for [WebTestClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class WebTestClientExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var requestBodySpec: WebTestClient.RequestBodySpec

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var responseSpec: WebTestClient.ResponseSpec


	@Test
	fun `RequestBodySpec#body with Publisher and reified type parameters`() {
		val body = mock<Publisher<Foo>>()
		requestBodySpec.body(body)
		verify(requestBodySpec, times(1)).body(body, Foo::class.java)
	}

	@Test
	fun `ResponseSpec#expectBody with reified type parameters`() {
		responseSpec.expectBody<Foo>()
		verify(responseSpec, times(1)).expectBody(Foo::class.java)
	}

	@Test
	fun `ResponseSpec#expectBodyList with reified type parameters`() {
		responseSpec.expectBodyList<Foo>()
		verify(responseSpec, times(1)).expectBodyList(Foo::class.java)
	}

	@Test
	fun `ResponseSpec#returnResult with reified type parameters`() {
		responseSpec.returnResult<Foo>()
		verify(responseSpec, times(1)).returnResult(Foo::class.java)
	}

	class Foo

}
