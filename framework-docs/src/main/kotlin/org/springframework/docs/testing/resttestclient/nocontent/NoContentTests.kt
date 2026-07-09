/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.docs.testing.resttestclient.nocontent

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

class NoContentTests {

	lateinit var client: RestTestClient

	@Test
	fun emptyBody() {
		val person = Person("Jane")
		// tag::emptyBody[]
		client.post().uri("/persons")
			.body(person)
			.exchange()
			.expectStatus().isCreated()
			.expectBody().isEmpty()
		// end::emptyBody[]
	}

	@Test
	fun ignoreBody() {
		val person = Person("Jane")
		// tag::ignoreBody[]
		client.get().uri("/persons/123")
			.exchange()
			.expectStatus().isNotFound
			.expectBody<Unit>()
		// end::ignoreBody[]
	}

	data class Person(val name: String)

}