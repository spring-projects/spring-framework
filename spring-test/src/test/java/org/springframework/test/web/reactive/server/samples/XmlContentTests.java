/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.hamcrest.Matchers;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Samples of tests using {@link WebTestClient} with serialized XML content.
 *
 * @author Eric Deandrea
 * @since 5.1
 */
public class XmlContentTests {

	private static final String PEOPLE_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
			+ "<people><people>"
			+ "<person><name>Jane</name></person>"
			+ "<person><name>Jason</name></person>"
			+ "<person><name>John</name></person>"
			+ "</people></people>";

	private final WebTestClient client = WebTestClient.bindToController(new PersonController()).build();

	@Test
	public void xmlContent() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody().xml(PEOPLE_XML);
	}

	@Test
	public void xpathIsEqualTo() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("/").exists()
				.xpath("/people").exists()
				.xpath("/people/people").exists()
				.xpath("/people/people/person").exists()
				.xpath("/people/people/person").nodeCount(3)
				.xpath("/people/people/person[1]/name").isEqualTo("Jane")
				.xpath("/people/people/person[2]/name").isEqualTo("Jason")
				.xpath("/people/people/person[3]/name").isEqualTo("John");
	}

	@Test
	public void xpathMatches() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("/").exists()
				.xpath("/people").exists()
				.xpath("/people/people").exists()
				.xpath("/people/people/person").exists()
				.xpath("/people/people/person").nodeCount(3)
				.xpath("//person/name").matchesString(Matchers.startsWith("J"));
	}

	@Test
	public void xpathContainsSubstringViaRegex() {
		this.client.get().uri("/persons/John")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("//name[contains(text(), 'oh')]").exists();
	}

	@Test
	public void postXmlContent() {
		this.client.post().uri("/persons")
				.contentType(MediaType.APPLICATION_XML)
				.syncBody("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><person><name>John</name></person>")
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().valueEquals(HttpHeaders.LOCATION, "/persons/John")
				.expectBody().isEmpty();
	}

	@XmlRootElement
	private static class Person {

		@NotNull
		private String name;

		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Person name(String name) {
			setName(name);
			return this;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Person)) {
				return false;
			}
			Person otherPerson = (Person) other;
			return ObjectUtils.nullSafeEquals(this.name, otherPerson.name);
		}

		@Override
		public int hashCode() {
			return Person.class.hashCode();
		}

		@Override
		public String toString() {
			return "Person [name=" + this.name + "]";
		}
	}

	@SuppressWarnings("unused")
	@XmlRootElement(name="people")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PeopleWrapper {

		@XmlElementWrapper(name="people")
		@XmlElement(name="person")
		private final List<Person> people = new ArrayList<>();

		public PeopleWrapper() {
		}

		public PeopleWrapper(List<Person> people) {
			this.people.addAll(people);
		}

		public PeopleWrapper(Person... people) {
			this.people.addAll(Arrays.asList(people));
		}

		public List<Person> getPeople() {
			return this.people;
		}
	}

	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
		Mono<PeopleWrapper> getPersons() {
			return Mono.just(new PeopleWrapper(new Person("Jane"), new Person("Jason"), new Person("John")));
		}

		@GetMapping(path = "/{name}", produces = MediaType.APPLICATION_XML_VALUE)
		Mono<Person> getPerson(@PathVariable String name) {
			return Mono.just(new Person(name));
		}

		@PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
		Mono<ResponseEntity<Object>> savePeople(@RequestBody Flux<Person> person) {
			return person
					.map(Person::getName)
					.map(name -> String.format("/persons/%s", name))
					.map(URI::create)
					.map(ResponseEntity::created)
					.map(ResponseEntity.BodyBuilder::build)
					.next();
		}
	}

}