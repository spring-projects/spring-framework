/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.servlet.client.samples;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsString;

/**
 * Samples of tests using {@link RestTestClient} with serialized JSON content.
 *
 * @author Rob Worsnop
 */
class JsonContentTests {

	private final RestTestClient client = RestTestClient.bindToController(new PersonController()).build();


	@Test
	void jsonContentWithDefaultLenientMode() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("""
						[
							{"firstName":"Jane"},
							{"firstName":"Jason"},
							{"firstName":"John"}
						]
						""");
	}

	@Test
	void jsonContentWithStrictMode() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody().json("""
						[
							{"firstName":"Jane", "lastName":"Williams"},
							{"firstName":"Jason","lastName":"Johnson"},
							{"firstName":"John", "lastName":"Smith"}
						]
						""",
						JsonCompareMode.STRICT);
	}

	@Test
	void jsonContentWithStrictModeAndMissingAttributes() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectBody().json("""
						[
							{"firstName":"Jane"},
							{"firstName":"Jason"},
							{"firstName":"John"}
						]
						""",
						JsonCompareMode.STRICT)
		);
	}

	@Test
	void jsonPathIsEqualTo() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$[0].firstName").isEqualTo("Jane")
				.jsonPath("$[1].firstName").isEqualTo("Jason")
				.jsonPath("$[2].firstName").isEqualTo("John");
	}

	@Test
	void jsonPathMatches() {
		this.client.get().uri("/persons/John/Smith")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.jsonPath("$.firstName").value(containsString("oh"));
	}

	@Test
	void postJsonContent() {
		this.client.post().uri("/persons")
				.contentType(MediaType.APPLICATION_JSON)
				.body("""
						{"firstName":"John", "lastName":"Smith"}
						""")
				.exchange()
				.expectStatus().isCreated()
				.expectBody().isEmpty();
	}


	@RestController
	@RequestMapping("/persons")
	private static class PersonController {

		@GetMapping
		List<Person> getPersons() {
			return List.of(new Person("Jane", "Williams"), new Person("Jason", "Johnson"), new Person("John", "Smith"));
		}

		@GetMapping("/{firstName}/{lastName}")
		Person getPerson(@PathVariable String firstName, @PathVariable String lastName) {
			return new Person(firstName, lastName);
		}

		@PostMapping
		ResponseEntity<String> savePerson(@RequestBody Person person) {
			URI location = URI.create(String.format("/persons/%s/%s", person.getFirstName(), person.getLastName()));
			return ResponseEntity.created(location).build();
		}
	}


	private static class Person {
		private String firstName;
		private String lastName;

		public Person() {
		}

		public Person(String firstName, String lastName) {
			this.firstName = firstName;
			this.lastName = lastName;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public String getLastName() {
			return this.lastName;
		}
	}

}
