/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.http.server.reactive.ZeroCopyIntegrationTests;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.WebReactiveConfiguration;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_XML;


/**
 * {@code @RequestMapping} integration tests focusing on serialization and
 * deserialization of the request and response body.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class RequestMappingMessageConversionIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private static final ParameterizedTypeReference<List<Person>> PERSON_LIST =
			new ParameterizedTypeReference<List<Person>>() {};

	private static final MediaType JSON = MediaType.APPLICATION_JSON;


	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void rawPojoResponse() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/raw", JSON, Person.class).getBody());
	}

	@Test
	public void rawFluxResponse() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-flux", null, String.class).getBody());
	}

	@Test
	public void rawObservableResponse() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-observable", null, String.class).getBody());
	}

	@Test
	public void serializeAsPojo() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person", JSON, Person.class).getBody());
	}

	@Test
	public void serializeAsCompletableFuture() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/completable-future", JSON, Person.class).getBody());
	}

	@Test
	public void serializeAsMono() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/mono", JSON, Person.class).getBody());
	}

	@Test
	public void serializeAsSingle() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/single", JSON, Person.class).getBody());
	}

	@Test
	public void serializeAsMonoResponseEntity() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/monoResponseEntity", JSON, Person.class).getBody());
	}

	@Test
	public void serializeAsList() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/list", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void serializeAsPublisher() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/publisher", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void serializeAsFlux() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/flux", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void serializeAsObservable() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/observable", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void resource() throws Exception {
		ResponseEntity<byte[]> response = performGet("/resource", null, byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.hasBody());
		assertEquals(951, response.getHeaders().getContentLength());
		assertEquals(951, response.getBody().length);
		assertEquals(new MediaType("image", "x-png"), response.getHeaders().getContentType());
	}

	@Test
	public void personCapitalize() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-capitalize", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void completableFutureCapitalize() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/completable-future-capitalize", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void monoCapitalize() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/mono-capitalize", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void singleCapitalize() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/single-capitalize", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void publisherCapitalize() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/publisher-capitalize", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void fluxCapitalize() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/flux-capitalize", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void observableCapitalize() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/observable-capitalize", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void publisherCreate() throws Exception {
		ResponseEntity<Void> entity = performPost("/publisher-create", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}

	@Test
	public void publisherCreateXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/publisher-create", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}

	@Test
	public void fluxCreate() throws Exception {
		ResponseEntity<Void> entity = performPost("/flux-create", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}

	@Test
	public void fluxCreateXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/flux-create", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}

	@Test
	public void observableCreate() throws Exception {
		ResponseEntity<Void> entity = performPost("/observable-create", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}

	@Test
	public void observableCreateXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/observable-create", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(TestRestController.class).persons.size());
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/RequestMappingMessageConversionIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebReactiveConfiguration {
	}


	@RestController
	@SuppressWarnings("unused")
	private static class TestRestController {

		final List<Person> persons = new ArrayList<>();

		// GET with "raw" data (DataBuffer) response body

		@GetMapping("/raw")
		public Publisher<ByteBuffer> rawResponseBody() {
			DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
			JacksonJsonEncoder encoder = new JacksonJsonEncoder();
			return encoder.encode(Mono.just(new Person("Robert")), dataBufferFactory,
					ResolvableType.forClass(Person.class), JSON).map(DataBuffer::asByteBuffer);
		}

		@GetMapping("/raw-flux")
		public Flux<ByteBuffer> rawFluxResponseBody() {
			return Flux.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/raw-observable")
		public Observable<ByteBuffer> rawObservableResponseBody() {
			return Observable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		// GET with Person Object(s) response body to "serialize"

		@GetMapping("/person")
		public Person personResponseBody() {
			return new Person("Robert");
		}

		@GetMapping("/completable-future")
		public CompletableFuture<Person> completableFutureResponseBody() {
			return CompletableFuture.completedFuture(new Person("Robert"));
		}

		@GetMapping("/mono")
		public Mono<Person> monoResponseBody() {
			return Mono.just(new Person("Robert"));
		}

		@GetMapping("/single")
		public Single<Person> singleResponseBody() {
			return Single.just(new Person("Robert"));
		}

		@GetMapping("/monoResponseEntity")
		public ResponseEntity<Mono<Person>> monoResponseEntity() {
			Mono<Person> body = Mono.just(new Person("Robert"));
			return ResponseEntity.ok(body);
		}

		@GetMapping("/list")
		public List<Person> listResponseBody() {
			return asList(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/publisher")
		public Publisher<Person> publisherResponseBody() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/flux")
		public Flux<Person> fluxResponseBody() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/observable")
		public Observable<Person> observableResponseBody() {
			return Observable.just(new Person("Robert"), new Person("Marie"));
		}

		// GET with Resource response body

		@GetMapping("/resource")
		public Resource resource() {
			return new ClassPathResource("spring.png", ZeroCopyIntegrationTests.class);
		}

		// POST with Person "capitalize" name transformation

		@PostMapping("/person-capitalize")
		public Person personCapitalize(@RequestBody Person person) {
			return new Person(person.getName().toUpperCase());
		}

		@PostMapping("/completable-future-capitalize")
		public CompletableFuture<Person> completableFutureCapitalize(
				@RequestBody CompletableFuture<Person> personFuture) {
			return personFuture.thenApply(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/mono-capitalize")
		public Mono<Person> monoCapitalize(@RequestBody Mono<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/single-capitalize")
		public Single<Person> singleCapitalize(@RequestBody Single<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/publisher-capitalize")
		public Publisher<Person> publisherCapitalize(@RequestBody Publisher<Person> persons) {
			return Flux
					.from(persons)
					.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flux-capitalize")
		public Flux<Person> fluxCapitalize(@RequestBody Flux<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/observable-capitalize")
		public Observable<Person> observableCapitalize(@RequestBody Observable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		// POST with Objects to "create"

		@PostMapping("/stream-create")
		public Publisher<Void> streamCreate(@RequestBody Flux<Person> personStream) {
			return personStream.collectList().doOnSuccess(persons::addAll).then();
		}

		@PostMapping("/publisher-create")
		public Publisher<Void> publisherCreate(@RequestBody Publisher<Person> personStream) {
			return Flux.from(personStream).doOnNext(persons::add).then();
		}

		@PostMapping("/flux-create")
		public Mono<Void> fluxCreate(@RequestBody Flux<Person> personStream) {
			return personStream.doOnNext(persons::add).then();
		}

		@PostMapping("/observable-create")
		public Observable<Void> observableCreate(@RequestBody Observable<Person> personStream) {
			return personStream.toList().doOnNext(persons::addAll).flatMap(document -> Observable.empty());
		}
	}

	@XmlRootElement @SuppressWarnings("WeakerAccess")
	private static class Person {

		private String name;

		@SuppressWarnings("unused")
		public Person() {
		}

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return !(this.name != null ? !this.name.equals(person.name) : person.name != null);
		}

		@Override
		public int hashCode() {
			return this.name != null ? this.name.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Person{" +
					"name='" + name + '\'' +
					'}';
		}
	}

	@XmlRootElement @SuppressWarnings({"WeakerAccess", "unused"})
	private static class People {

		private List<Person> persons = new ArrayList<>();

		public People() {
		}

		public People(Person... persons) {
			this.persons.addAll(Arrays.asList(persons));
		}

		@XmlElement
		public List<Person> getPerson() {
			return this.persons;
		}

	}

}
