/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server.samples;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

/**
 * Annotated controllers accepting and returning typed Objects.
 *
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unused")
public class ResponseEntityTests {

	private WebTestClient client;


	@Before
	public void setUp() throws Exception {
		this.client = WebTestClient.bindToController(new PersonController()).build();
	}


	@Test
	public void entity() throws Exception {
		this.client.get().uri("/persons/John")
				.exchange()
				.assertStatus().isOk()
				.assertHeaders().contentType(MediaType.APPLICATION_JSON_UTF8)
				.assertEntity(Person.class).isEqualTo(new Person("John"));
	}

	@Test
	public void entityCollection() throws Exception {
		this.client.get().uri("/persons")
				.exchange()
				.assertStatus().isOk()
				.assertHeaders().contentType(MediaType.APPLICATION_JSON_UTF8)
				.assertEntity(Person.class).list()
				.hasSize(3)
				.contains(new Person("Jane"), new Person("Jason"), new Person("John"));
	}

	@Test
	public void entityStream() throws Exception {
		this.client.get().uri("/persons").accept(TEXT_EVENT_STREAM)
				.exchange()
				.assertStatus().isOk()
				.assertHeaders().contentType(TEXT_EVENT_STREAM)
				.assertEntity(Person.class).stepVerifier()
				.expectNext(new Person("N0"), new Person("N1"), new Person("N2"))
				.expectNextCount(4)
				.consumeNextWith(person -> assertThat(person.getName(), endsWith("7")))
				.thenCancel()
				.verify();
	}

	@Test
	public void saveEntity() throws Exception {
		this.client.post().uri("/persons")
				.exchange(Mono.just(new Person("John")), Person.class)
				.assertStatus().isCreated()
				.assertHeader("location").isEqualTo("/persons/John").and()
				.assertBody().isEmpty();
	}


	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping("/{name}")
		Person getPerson(@PathVariable String name) {
			return new Person(name);
		}

		@GetMapping
		Flux<Person> getPersons() {
			return Flux.just(new Person("Jane"), new Person("Jason"), new Person("John"));
		}

		@GetMapping(produces = "text/event-stream")
		Flux<Person> getPersonStream() {
			return Flux.intervalMillis(100).onBackpressureBuffer(10).map(index -> new Person("N" + index));
		}

		@PostMapping
		ResponseEntity<String> savePerson(@RequestBody Person person) {
			return ResponseEntity.created(URI.create("/persons/" + person.getName())).build();
		}
	}

	static class Person {

		private final String name;

		@JsonCreator
		public Person(@JsonProperty("name") String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) return true;
			if (other == null || getClass() != other.getClass()) return false;
			Person person = (Person) other;
			return getName().equals(person.getName());
		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public String toString() {
			return "Person[name='" + name + "']";
		}
	}

}
