/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.client.samples;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;

/**
 * Annotated controllers accepting and returning typed Objects.
 *
 * @author Rob Worsnop
 */
class ResponseEntityTests {

	private final RestTestClient client = RestTestClient.bindToController(new PersonController())
			.baseUrl("/persons")
			.build();


	@Test
	void entity() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class).isEqualTo(new Person("John"));
	}

	@Test
	void entityMatcher() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class).value(Person::getName, name -> MatcherAssert.assertThat(name, startsWith("Joh")));
	}

	@Test
	void entityWithConsumer() {
		this.client.get().uri("/John")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(Person.class)
				.consumeWith(result -> assertThat(result.getResponseBody()).isEqualTo(new Person("John")));
	}

	@Test
	void entityList() {
		List<Person> expected = List.of(
				new Person("Jane"), new Person("Jason"), new Person("John"));

		this.client.get()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(new ParameterizedTypeReference<List<Person>>() {}).isEqualTo(expected);
	}

	@Test
	void entityListWithConsumer() {
		this.client.get()
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody(new ParameterizedTypeReference<List<Person>>() {})
				.value(people ->
					assertThat(people).contains(new Person("Jason"))
				);
	}

	@Test
	void entityMap() {
		Map<String, Person> map = new LinkedHashMap<>();
		map.put("Jane", new Person("Jane"));
		map.put("Jason", new Person("Jason"));
		map.put("John", new Person("John"));

		this.client.get().uri("?map=true")
				.exchange()
				.expectStatus().isOk()
				.expectBody(new ParameterizedTypeReference<Map<String, Person>>() {}).isEqualTo(map);
	}

	@Test
	void postEntity() {
		this.client.post()
				.contentType(MediaType.APPLICATION_JSON)
				.body(new Person("John"))
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().valueEquals("location", "/persons/John")
				.expectBody().isEmpty();
	}


	@RestController
	@RequestMapping("/persons")
	private static class PersonController {

		@GetMapping(path = "/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
		Person getPerson(@PathVariable String name) {
			return new Person(name);
		}

		@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
		List<Person> getPersons() {
			return List.of(new Person("Jane"), new Person("Jason"), new Person("John"));
		}

		@GetMapping(params = "map", produces = MediaType.APPLICATION_JSON_VALUE)
		Map<String, Person> getPersonsAsMap() {
			Map<String, Person> map = new LinkedHashMap<>();
			map.put("Jane", new Person("Jane"));
			map.put("Jason", new Person("Jason"));
			map.put("John", new Person("John"));
			return map;
		}

		@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
		ResponseEntity<String> savePerson(@RequestBody Person person) {
			return ResponseEntity.created(URI.create("/persons/" + person.getName())).build();
		}
	}

}
