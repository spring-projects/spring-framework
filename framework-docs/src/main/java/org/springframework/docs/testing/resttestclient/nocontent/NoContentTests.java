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

package org.springframework.docs.testing.resttestclient.nocontent;

import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.client.RestTestClient;

public class NoContentTests {


	RestTestClient client;

	@Test
	void emptyBody() {
		Person person = new Person("Jane");
		// tag::emptyBody[]
		client.post().uri("/persons")
				.body(person)
				.exchange()
				.expectStatus().isCreated()
				.expectBody().isEmpty();
		// end::emptyBody[]
	}

	@Test
	void ignoreBody() {
		Person person = new Person("Jane");
		// tag::ignoreBody[]
		client.post().uri("/persons")
				.body(person)
				.exchange()
				.expectStatus().isCreated()
				.expectBody(Void.class);
		// end::ignoreBody[]
	}

	record Person(String name) {

	}

}
