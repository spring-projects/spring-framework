/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.workflow

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.client.expectBody

class RestClientWorkflowTests {

	lateinit var client: RestTestClient

	@Test
	fun workflowTest() {
		// tag::test[]
		client.get().uri("/persons/1")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType(MediaType.APPLICATION_JSON)
			.expectBody()
		// end::test[]
	}

	@Test
	fun softAssertions() {
		// tag::soft-assertions[]
		client.get().uri("/persons/1")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectAll(
				{ spec -> spec.expectStatus().isOk() },
				{ spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON) }
			)
		// end::soft-assertions[]
	}

	@Test
	fun consumeWith() {
		// tag::consume[]
		client.get().uri("/persons/1")
			.exchange()
			.expectStatus().isOk()
			.expectBody<Person>()
			.consumeWith {
				// custom assertions (for example, AssertJ)...
			}
		// end::consume[]
	}

	@Test
	fun returnResult() {
		// tag::result[]
		val result = client.get().uri("/persons/1")
			.exchange()
			.expectStatus().isOk
			.expectBody<Person>()
			.returnResult()

		val person: Person? = result.responseBody
		val requestHeaders = result.responseHeaders
		// end::result[]
	}

	data class Person(val name: String)

}
