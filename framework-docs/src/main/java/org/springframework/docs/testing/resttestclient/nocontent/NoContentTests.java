/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
