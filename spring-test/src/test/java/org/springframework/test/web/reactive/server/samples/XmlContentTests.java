/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.reactive.server.samples;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.*;

/**
 * Samples of tests using {@link WebTestClient} with XML content.
 *
 * @author Eric Deandrea
 * @since 5.1
 */
public class XmlContentTests {

	private static final String persons_XML =
			"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
			+ "<persons>"
			+ "<person><name>Jane</name></person>"
			+ "<person><name>Jason</name></person>"
			+ "<person><name>John</name></person>"
			+ "</persons>";


	private final WebTestClient client = WebTestClient.bindToController(new PersonController()).build();


	@Test
	public void xmlContent() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody().xml(persons_XML);
	}

	@Test
	public void xpathIsEqualTo() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("/").exists()
				.xpath("/persons").exists()
				.xpath("/persons/person").exists()
				.xpath("/persons/person").nodeCount(3)
				.xpath("/persons/person[1]/name").isEqualTo("Jane")
				.xpath("/persons/person[2]/name").isEqualTo("Jason")
				.xpath("/persons/person[3]/name").isEqualTo("John");
	}

	@Test
	public void xpathMatches() {
		this.client.get().uri("/persons")
				.accept(MediaType.APPLICATION_XML)
				.exchange()
				.expectStatus().isOk()
				.expectBody()
				.xpath("//person/name").string(startsWith("J"));
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

		String content =
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
				"<person><name>John</name></person>";

		this.client.post().uri("/persons")
				.contentType(MediaType.APPLICATION_XML)
				.syncBody(content)
				.exchange()
				.expectStatus().isCreated()
				.expectHeader().valueEquals(HttpHeaders.LOCATION, "/persons/John")
				.expectBody().isEmpty();
	}


	@SuppressWarnings("unused")
	@XmlRootElement(name="persons")
	@XmlAccessorType(XmlAccessType.FIELD)
	private static class PersonsWrapper {

		@XmlElement(name="person")
		private final List<Person> persons = new ArrayList<>();

		public PersonsWrapper() {
		}

		public PersonsWrapper(List<Person> persons) {
			this.persons.addAll(persons);
		}

		public PersonsWrapper(Person... persons) {
			this.persons.addAll(Arrays.asList(persons));
		}

		public List<Person> getpersons() {
			return this.persons;
		}
	}

	@RestController
	@RequestMapping("/persons")
	static class PersonController {

		@GetMapping(produces = MediaType.APPLICATION_XML_VALUE)
		PersonsWrapper getPersons() {
			return new PersonsWrapper(new Person("Jane"), new Person("Jason"), new Person("John"));
		}

		@GetMapping(path = "/{name}", produces = MediaType.APPLICATION_XML_VALUE)
		Person getPerson(@PathVariable String name) {
			return new Person(name);
		}

		@PostMapping(consumes = MediaType.APPLICATION_XML_VALUE)
		ResponseEntity<Object> savepersons(@RequestBody Person person) {
			URI location = URI.create(String.format("/persons/%s", person.getName()));
			return ResponseEntity.created(location).build();
		}
	}

}