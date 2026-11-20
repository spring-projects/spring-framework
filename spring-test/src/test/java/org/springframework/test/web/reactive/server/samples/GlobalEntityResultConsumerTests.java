/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server.samples;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests with a globally registered
 * {@link org.springframework.test.web.reactive.server.EntityExchangeResult} consumer.
 *
 * @author Rossen Stoyanchev
 */
class GlobalEntityResultConsumerTests {

	private final StringBuilder output = new StringBuilder();

	private final WebTestClient client = WebTestClient.bindToController(TestController.class)
			.configureClient()
			.entityExchangeResultConsumer(result -> {
				byte[] bytes = result.getResponseBodyContent();
				this.output.append(new String(bytes, StandardCharsets.UTF_8));
			})
			.build();


	@Test
	void json() {
		this.client.get().uri("/person/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("{\"name\":\"Joe\"}");

		assertThat(this.output.toString()).isEqualTo("{\"name\":\"Joe\"}");
	}

	@Test
	void entity() {
		this.client.get().uri("/person/1")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody(Person.class).isEqualTo(new Person("Joe"));

		assertThat(this.output.toString()).isEqualTo("{\"name\":\"Joe\"}");
	}

	@Test
	void entityList() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBodyList(Person.class).hasSize(2);

		assertThat(this.output.toString())
				.isEqualTo("[{\"name\":\"Joe\"},{\"name\":\"Joseph\"}]");
	}


	@RestController
	static class TestController {

		@GetMapping("/person/{id}")
		Person getPerson() {
			return new Person("Joe");
		}

		@GetMapping("/persons")
		List<Person> getPersons() {
			return Arrays.asList(new Person("Joe"), new Person("Joseph"));
		}
	}

}
