/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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