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

package org.springframework.docs.testing.resttestclient.json

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.client.RestTestClient

class JsonTests {

	lateinit var client: RestTestClient

	@Test
	fun jsonBody() {
		// tag::jsonBody[]
		client.get().uri("/persons/1")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.json("{\"name\":\"Jane\"}")
		// end::jsonBody[]
	}

	@Test
	fun jsonPath() {
		// tag::jsonPath[]
		client.get().uri("/persons")
			.exchange()
			.expectStatus().isOk()
			.expectBody()
			.jsonPath("$[0].name").isEqualTo("Jane")
			.jsonPath("$[1].name").isEqualTo("Jason")
		// end::jsonPath[]
	}

}