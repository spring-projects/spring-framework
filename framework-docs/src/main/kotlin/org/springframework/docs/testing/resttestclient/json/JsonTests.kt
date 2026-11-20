/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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