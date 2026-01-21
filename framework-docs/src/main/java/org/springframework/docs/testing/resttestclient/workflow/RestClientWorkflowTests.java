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
