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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Completable;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.ZeroCopyIntegrationTests;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebReactive;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
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
	public void byteBufferResponseBodyWithPublisher() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/raw-response/publisher", JSON, Person.class).getBody());
	}

	@Test
	public void byteBufferResponseBodyWithFlux() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-response/flux", new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void byteBufferResponseBodyWithObservable() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-response/observable", new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void byteBufferResponseBodyWithRxJava2Observable() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-response/rxjava2-observable",
				new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void byteBufferResponseBodyWithFlowable() throws Exception {
		String expected = "Hello!";
		assertEquals(expected, performGet("/raw-response/flowable", new HttpHeaders(), String.class).getBody());
	}

	@Test
	public void personResponseBody() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person-response/person", JSON, Person.class).getBody());
	}

	@Test
	public void personResponseBodyWithCompletableFuture() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person-response/completable-future", JSON, Person.class).getBody());
	}

	@Test
	public void personResponseBodyWithMono() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person-response/mono", JSON, Person.class).getBody());
	}

	@Test
	public void personResponseBodyWithSingle() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person-response/single", JSON, Person.class).getBody());
	}

	@Test
	public void personResponseBodyWithMonoResponseEntity() throws Exception {
		Person expected = new Person("Robert");
		assertEquals(expected, performGet("/person-response/mono-response-entity", JSON, Person.class).getBody());
	}

	@Test
	public void personResponseBodyWithList() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/person-response/list", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personResponseBodyWithPublisher() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/person-response/publisher", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personResponseBodyWithFlux() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/person-response/flux", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personResponseBodyWithObservable() throws Exception {
		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertEquals(expected, performGet("/person-response/observable", JSON, PERSON_LIST).getBody());
	}

	@Test
	public void resource() throws Exception {

		// SPR-14975
		assumeFalse(server instanceof ReactorHttpServer);

		ResponseEntity<byte[]> response = performGet("/resource", new HttpHeaders(), byte[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertTrue(response.hasBody());
		assertEquals(951, response.getHeaders().getContentLength());
		assertEquals(951, response.getBody().length);
		assertEquals(new MediaType("image", "x-png"), response.getHeaders().getContentType());
	}

	@Test
	public void personTransform() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/person", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithCompletableFuture() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/completable-future", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithMono() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/mono", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithSingle() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/single", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithRxJava2Single() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/rxjava2-single", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithRxJava2Maybe() throws Exception {
		assertEquals(new Person("ROBERT"),
				performPost("/person-transform/rxjava2-maybe", JSON, new Person("Robert"),
						JSON, Person.class).getBody());
	}

	@Test
	public void personTransformWithPublisher() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/person-transform/publisher", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personTransformWithFlux() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/person-transform/flux", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personTransformWithObservable() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/person-transform/observable", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personTransformWithRxJava2Observable() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/person-transform/rxjava2-observable", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personTransformWithFlowable() throws Exception {
		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertEquals(res, performPost("/person-transform/flowable", JSON, req, JSON, PERSON_LIST).getBody());
	}

	@Test
	public void personCreateWithPublisherJson() throws Exception {
		ResponseEntity<Void> entity = performPost("/person-create/publisher", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithPublisherXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/publisher", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithMono() throws Exception {
		ResponseEntity<Void> entity = performPost(
				"/person-create/mono", JSON, new Person("Robert"), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithSingle() throws Exception {
		ResponseEntity<Void> entity = performPost(
				"/person-create/single", JSON, new Person("Robert"), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithRxJava2Single() throws Exception {
		ResponseEntity<Void> entity = performPost(
				"/person-create/rxjava2-single", JSON, new Person("Robert"), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithFluxJson() throws Exception {
		ResponseEntity<Void> entity = performPost("/person-create/flux", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithFluxXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flux", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithObservableJson() throws Exception {
		ResponseEntity<Void> entity = performPost("/person-create/observable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithRxJava2ObservableJson() throws Exception {
		ResponseEntity<Void> entity = performPost("/person-create/rxjava2-observable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithObservableXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/observable", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithRxJava2ObservableXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/rxjava2-observable", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithFlowableJson() throws Exception {
		ResponseEntity<Void> entity = performPost("/person-create/flowable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}

	@Test
	public void personCreateWithFlowableXml() throws Exception {
		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flowable", APPLICATION_XML, people, null, Void.class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(2, getApplicationContext().getBean(PersonCreateController.class).persons.size());
	}


	@Configuration
	@EnableWebReactive
	@ComponentScan(resourcePattern = "**/RequestMappingMessageConversionIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController
	@RequestMapping("/raw-response")
	@SuppressWarnings("unused")
	private static class RawResponseBodyController {

		@GetMapping("/publisher")
		public Publisher<ByteBuffer> getPublisher() {
			DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
			Jackson2JsonEncoder encoder = new Jackson2JsonEncoder();
			return encoder.encode(Mono.just(new Person("Robert")), dataBufferFactory,
					ResolvableType.forClass(Person.class), JSON, Collections.emptyMap()).map(DataBuffer::asByteBuffer);
		}

		@GetMapping("/flux")
		public Flux<ByteBuffer> getFlux() {
			return Flux.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/observable")
		public Observable<ByteBuffer> getObservable() {
			return Observable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/rxjava2-observable")
		public io.reactivex.Observable<ByteBuffer> getRxJava2Observable() {
			return io.reactivex.Observable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/flowable")
		public Flowable<ByteBuffer> getFlowable() {
			return Flowable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}
	}


	@RestController
	@RequestMapping("/person-response")
	@SuppressWarnings("unused")
	private static class PersonResponseBodyController {

		@GetMapping("/person")
		public Person getPerson() {
			return new Person("Robert");
		}

		@GetMapping("/completable-future")
		public CompletableFuture<Person> getCompletableFuture() {
			return CompletableFuture.completedFuture(new Person("Robert"));
		}

		@GetMapping("/mono")
		public Mono<Person> getMono() {
			return Mono.just(new Person("Robert"));
		}

		@GetMapping("/single")
		public Single<Person> getSingle() {
			return Single.just(new Person("Robert"));
		}

		@GetMapping("/mono-response-entity")
		public ResponseEntity<Mono<Person>> getMonoResponseEntity() {
			Mono<Person> body = Mono.just(new Person("Robert"));
			return ResponseEntity.ok(body);
		}

		@GetMapping("/list")
		public List<Person> getList() {
			return asList(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/publisher")
		public Publisher<Person> getPublisher() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/flux")
		public Flux<Person> getFlux() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/observable")
		public Observable<Person> getObservable() {
			return Observable.just(new Person("Robert"), new Person("Marie"));
		}
	}


	@RestController
	@SuppressWarnings("unused")
	private static class ResourceController {

		@GetMapping("/resource")
		public Resource resource() {
			return new ClassPathResource("spring.png", ZeroCopyIntegrationTests.class);
		}
	}


	@RestController
	@RequestMapping("/person-transform")
	@SuppressWarnings("unused")
	private static class PersonTransformationController {

		@PostMapping("/person")
		public Person transformPerson(@RequestBody Person person) {
			return new Person(person.getName().toUpperCase());
		}

		@PostMapping("/completable-future")
		public CompletableFuture<Person> transformCompletableFuture(
				@RequestBody CompletableFuture<Person> personFuture) {
			return personFuture.thenApply(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/mono")
		public Mono<Person> transformMono(@RequestBody Mono<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/single")
		public Single<Person> transformSingle(@RequestBody Single<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/rxjava2-single")
		public io.reactivex.Single<Person> transformRxJava2Single(@RequestBody io.reactivex.Single<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/rxjava2-maybe")
		public Maybe<Person> transformRxJava2Maybe(@RequestBody Maybe<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/publisher")
		public Publisher<Person> transformPublisher(@RequestBody Publisher<Person> persons) {
			return Flux
					.from(persons)
					.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flux")
		public Flux<Person> transformFlux(@RequestBody Flux<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/observable")
		public Observable<Person> transformObservable(@RequestBody Observable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/rxjava2-observable")
		public io.reactivex.Observable<Person> transformObservable(@RequestBody io.reactivex.Observable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flowable")
		public Flowable<Person> transformFlowable(@RequestBody Flowable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}
	}


	@RestController
	@RequestMapping("/person-create")
	@SuppressWarnings("unused")
	private static class PersonCreateController {

		final List<Person> persons = new ArrayList<>();

		@PostMapping("/publisher")
		public Publisher<Void> createWithPublisher(@RequestBody Publisher<Person> publisher) {
			return Flux.from(publisher).doOnNext(persons::add).then();
		}

		@PostMapping("/mono")
		public Mono<Void> createWithMono(@RequestBody Mono<Person> mono) {
			return mono.doOnNext(persons::add).then();
		}

		@PostMapping("/single")
		public Completable createWithSingle(@RequestBody Single<Person> single) {
			return single.map(persons::add).toCompletable();
		}

		@PostMapping("/rxjava2-single")
		public io.reactivex.Completable createWithRxJava2Single(@RequestBody io.reactivex.Single<Person> single) {
			return single.map(persons::add).toCompletable();
		}

		@PostMapping("/flux")
		public Mono<Void> createWithFlux(@RequestBody Flux<Person> flux) {
			return flux.doOnNext(persons::add).then();
		}

		@PostMapping("/observable")
		public Observable<Void> createWithObservable(@RequestBody Observable<Person> observable) {
			return observable.toList().doOnNext(persons::addAll).flatMap(document -> Observable.empty());
		}

		@PostMapping("/rxjava2-observable")
		public io.reactivex.Completable createWithRxJava2Observable(@RequestBody io.reactivex.Observable<Person> observable) {
			return observable.toList().doOnSuccess(persons::addAll).toCompletable();
		}

		@PostMapping("/flowable")
		public io.reactivex.Completable createWithFlowable(@RequestBody Flowable<Person> flowable) {
			return flowable.toList().doOnSuccess(persons::addAll).toCompletable();
		}
	}


	@XmlRootElement
	@SuppressWarnings("WeakerAccess")
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


	@XmlRootElement
	@SuppressWarnings({"WeakerAccess", "unused"})
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
