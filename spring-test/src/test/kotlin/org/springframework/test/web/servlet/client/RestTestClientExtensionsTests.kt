/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client

import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Mock object based tests for [RestTestClient] Kotlin extensions
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 */
class RestTestClientExtensionsTests {

	private val responseSpec = mockk<RestTestClient.ResponseSpec>(relaxed = true)


	@Test
	fun `ResponseSpec#expectBody with reified type parameters`() {
		responseSpec.expectBody<Foo>()
		verify { responseSpec.expectBody(object : ParameterizedTypeReference<Foo>() {}) }
	}

	@Test
	fun `KotlinBodySpec#isEqualTo`() {
		RestTestClient
				.bindToController(TestController())
				.build()
				.get().uri("/").exchange().expectBody<String>().isEqualTo("foo")
	}

	@Test
	fun `KotlinBodySpec#consumeWith`() {
		RestTestClient
				.bindToController(TestController())
				.build()
				.get().uri("/").exchange().expectBody<String>()
				.consumeWith { assertThat(it.responseBody).isEqualTo("foo") }
	}

	@Test
	fun `KotlinBodySpec#returnResult`() {
		RestTestClient
				.bindToController(TestController())
				.build()
				.get().uri("/").exchange().expectBody<String>().returnResult()
				.apply { assertThat(responseBody).isEqualTo("foo") }
	}

	@Test
	fun `ResponseSpec#returnResult with reified type parameters`() {
		responseSpec.returnResult<Foo>()
		verify { responseSpec.returnResult(object : ParameterizedTypeReference<Foo>() {}) }
	}

	class Foo

	@RestController
	class TestController {

		@GetMapping("/")
		fun home() = "foo"

	}


}
