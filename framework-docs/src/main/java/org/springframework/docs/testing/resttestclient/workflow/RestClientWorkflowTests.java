/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.testing.resttestclient.workflow;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

public class RestClientWorkflowTests {

	RestTestClient client;

	@Test
	void workflowTest() {
		// tag::test[]
		client.get().uri("/persons/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody();
		// end::test[]
	}

	@Test
	void softAssertions() {
		// tag::soft-assertions[]
		client.get().uri("/persons/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectAll(
						spec -> spec.expectStatus().isOk(),
						spec -> spec.expectHeader().contentType(MediaType.APPLICATION_JSON)
				);
		// end::soft-assertions[]
	}

	@Test
	void consumeWith() {
		// tag::consume[]
		client.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Person.class)
				.consumeWith(result -> {
					// custom assertions (for example, AssertJ)...
				});
		// end::consume[]
	}

	@Test
	void returnResult() {
		// tag::result[]
		EntityExchangeResult<Person> result = client.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectBody(Person.class)
				.returnResult();

		Person person = result.getResponseBody();
		HttpHeaders requestHeaders = result.getRequestHeaders();
		// end::result[]
	}

	record Person(String name) {

	}
}
