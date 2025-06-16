/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_XML;

/**
 * {@code @RequestMapping} integration tests focusing on serialization and
 * deserialization of the request and response body.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class RequestMappingMessageConversionIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private static final ParameterizedTypeReference<List<Person>> PERSON_LIST =
			new ParameterizedTypeReference<>() {};

	private static final MediaType JSON = MediaType.APPLICATION_JSON;


	@Override
	protected ApplicationContext initApplicationContext() {
		return new AnnotationConfigApplicationContext(WebConfig.class);
	}


	@ParameterizedHttpServerTest
	void byteBufferResponseBodyWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		assertThat(performGet("/raw-response/publisher", JSON, Person.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void byteBufferResponseBodyWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/flux", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void byteBufferResponseBodyWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		ResponseEntity<String> responseEntity = performGet("/raw-response/mono", new HttpHeaders(), String.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(6);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void byteBufferResponseBodyWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/observable",
				new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void byteBufferResponseBodyWithFlowable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/flowable", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBody(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/person", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithCompletableFuture(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/completable-future", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/mono", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest // SPR-17506
	void personResponseBodyWithEmptyMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Person> responseEntity = performGet("/person-response/mono-empty", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(0);
		assertThat(responseEntity.getBody()).isNull();

		// As we're on the same connection, the 2nd request proves server response handling
		// did complete after the 1st request.
		responseEntity = performGet("/person-response/mono-empty", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(0);
		assertThat(responseEntity.getBody()).isNull();
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithMonoDeclaredAsObject(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/mono-declared-as-object", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/single", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithMonoResponseEntity(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/mono-response-entity", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest // SPR-16172
	void personResponseBodyWithMonoResponseEntityXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		String url = "/person-response/mono-response-entity-xml";
		ResponseEntity<String> entity = performGet(url, new HttpHeaders(), String.class);
		String actual = entity.getBody();

		assertThat(entity.getHeaders().getContentLength()).isEqualTo(91);
		assertThat(actual).isEqualTo(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
				"<person><name>Robert</name></person>"));
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithList(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		ResponseEntity<List<Person>> entity = performGet("/person-response/list", JSON, PERSON_LIST);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(36);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		ResponseEntity<List<Person>> entity = performGet("/person-response/publisher", JSON, PERSON_LIST);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(-1);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertThat(performGet("/person-response/flux", JSON, PERSON_LIST).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void personResponseBodyWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertThat(performGet("/person-response/observable", JSON, PERSON_LIST).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	void resource(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<byte[]> response = performGet("/resource", new HttpHeaders(), byte[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getHeaders().getContentLength()).isEqualTo(951);
		assertThat(response.getBody()).hasSize(951);
		assertThat(response.getHeaders().getContentType()).isEqualTo(new MediaType("image", "png"));
	}

	@ParameterizedHttpServerTest
	void personTransform(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/person", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	void personTransformWithCompletableFuture(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/completable-future", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	void personTransformWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/mono", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest  // SPR-16759
	void personTransformWithMonoAndXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/mono", MediaType.APPLICATION_XML, new Person("Robert"),
				MediaType.APPLICATION_XML, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	void personTransformWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/single", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	void personTransformWithMaybe(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/maybe", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	void personTransformWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/publisher", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	void personTransformWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/flux", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest // see gh-33885
	void personTransformWithFluxDelayed(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/flux-delayed", JSON, req, JSON, PERSON_LIST))
				.satisfies(r -> assertThat(r.getBody()).isEqualTo(res))
				.satisfies(r -> assertThat(r.getHeaders().getContentLength()).isNotZero());
	}

	@ParameterizedHttpServerTest
	void personTransformWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/observable", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	void personTransformWithFlowable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/flowable", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	void personCreateWithPublisherJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/publisher", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithPublisherXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/publisher", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost(
				"/person-create/mono", JSON, new Person("Robert"), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(1);
	}

	@ParameterizedHttpServerTest
	void personCreateWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost(
				"/person-create/single", JSON, new Person("Robert"), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(1);
	}

	@ParameterizedHttpServerTest
	void personCreateWithFluxJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/flux", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithFluxXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flux", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithObservableJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/observable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithObservableXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/observable", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithFlowableJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/flowable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest
	void personCreateWithFlowableXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flowable", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons).hasSize(2);
	}

	@ParameterizedHttpServerTest // gh-23791
	void personCreateViaDefaultMethodWithGenerics(HttpServer httpServer) throws Exception {
		startServer(httpServer);
		ResponseEntity<String> entity = performPost("/23791", JSON, new Person("Robert"), null, String.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Person");
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/RequestMappingMessageConversionIntegrationTests$*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController
	@RequestMapping("/raw-response")
	@SuppressWarnings("unused")
	private static class RawResponseBodyController {

		@GetMapping("/publisher")
		@SuppressWarnings("deprecation")
		Publisher<ByteBuffer> getPublisher() {
			JacksonJsonEncoder encoder = new JacksonJsonEncoder();
			return encoder.encode(Mono.just(new Person("Robert")), DefaultDataBufferFactory.sharedInstance,
					ResolvableType.forClass(Person.class), JSON, Collections.emptyMap()).map(DataBuffer::toByteBuffer);
		}

		@GetMapping("/flux")
		Flux<ByteBuffer> getFlux() {
			return Flux.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/mono")
		Mono<ByteBuffer> getMonoString() {
			return Mono.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/observable")
		Observable<ByteBuffer> getObservable() {
			return Observable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}

		@GetMapping("/flowable")
		Flowable<ByteBuffer> getFlowable() {
			return Flowable.just(ByteBuffer.wrap("Hello!".getBytes()));
		}
	}


	@RestController
	@RequestMapping("/person-response")
	@SuppressWarnings("unused")
	private static class PersonResponseBodyController {

		@GetMapping("/person")
		Person getPerson() {
			return new Person("Robert");
		}

		@GetMapping("/completable-future")
		CompletableFuture<Person> getCompletableFuture() {
			return CompletableFuture.completedFuture(new Person("Robert"));
		}

		@GetMapping("/mono")
		Mono<Person> getMono() {
			return Mono.just(new Person("Robert"));
		}

		@GetMapping("/mono-empty")
		Mono<Person> getMonoEmpty() {
			return Mono.empty();
		}

		@GetMapping("/mono-declared-as-object")
		Object getMonoDeclaredAsObject() {
			return Mono.just(new Person("Robert"));
		}

		@GetMapping("/single")
		Single<Person> getSingle() {
			return Single.just(new Person("Robert"));
		}

		@GetMapping("/mono-response-entity")
		ResponseEntity<Mono<Person>> getMonoResponseEntity() {
			Mono<Person> body = Mono.just(new Person("Robert"));
			return ResponseEntity.ok(body);
		}

		@GetMapping("/mono-response-entity-xml")
		ResponseEntity<Mono<Person>> getMonoResponseEntityXml() {
			Mono<Person> body = Mono.just(new Person("Robert"));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
		}

		@GetMapping("/list")
		List<Person> getList() {
			return asList(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/publisher")
		Publisher<Person> getPublisher() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/flux")
		Flux<Person> getFlux() {
			return Flux.just(new Person("Robert"), new Person("Marie"));
		}

		@GetMapping("/observable")
		Observable<Person> getObservable() {
			return Observable.just(new Person("Robert"), new Person("Marie"));
		}
	}


	@RestController
	@SuppressWarnings("unused")
	private static class ResourceController {

		@GetMapping("/resource")
		Resource resource() {
			return new ClassPathResource("/org/springframework/web/reactive/spring.png");
		}
	}


	@RestController
	@RequestMapping("/person-transform")
	@SuppressWarnings("unused")
	private static class PersonTransformationController {

		@PostMapping("/person")
		Person transformPerson(@RequestBody Person person) {
			return new Person(person.getName().toUpperCase());
		}

		@PostMapping("/completable-future")
		CompletableFuture<Person> transformCompletableFuture(@RequestBody CompletableFuture<Person> future) {
			return future.thenApply(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/mono")
		Mono<Person> transformMono(@RequestBody Mono<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/single")
		Single<Person> transformSingle(@RequestBody Single<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/maybe")
		Maybe<Person> transformMaybe(@RequestBody Maybe<Person> personFuture) {
			return personFuture.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/publisher")
		Publisher<Person> transformPublisher(@RequestBody Publisher<Person> persons) {
			return Flux.from(persons).map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flux")
		Flux<Person> transformFlux(@RequestBody Flux<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flux-delayed")
		Flux<Person> transformDelayed(@RequestBody Flux<Person> persons) {
			return transformFlux(persons).delayElements(Duration.ofMillis(10));
		}

		@PostMapping("/observable")
		Observable<Person> transformObservable(@RequestBody Observable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}

		@PostMapping("/flowable")
		Flowable<Person> transformFlowable(@RequestBody Flowable<Person> persons) {
			return persons.map(person -> new Person(person.getName().toUpperCase()));
		}
	}


	@RestController
	@RequestMapping("/person-create")
	@SuppressWarnings("unused")
	private static class PersonCreateController {

		final List<Person> persons = new ArrayList<>();

		@PostMapping("/publisher")
		Publisher<Void> createWithPublisher(@RequestBody Publisher<Person> publisher) {
			return Flux.from(publisher).doOnNext(persons::add).then();
		}

		@PostMapping("/mono")
		Mono<Void> createWithMono(@RequestBody Mono<Person> mono) {
			return mono.doOnNext(persons::add).then();
		}

		@PostMapping("/single")
		Completable createWithSingle(@RequestBody Single<Person> single) {
			return single.map(persons::add).ignoreElement();
		}

		@PostMapping("/flux")
		Mono<Void> createWithFlux(@RequestBody Flux<Person> flux) {
			return flux.doOnNext(persons::add).then();
		}

		@PostMapping("/observable")
		Completable createWithObservable(@RequestBody Observable<Person> observable) {
			return observable.toList().doOnSuccess(persons::addAll).ignoreElement();
		}

		@PostMapping("/flowable")
		Completable createWithFlowable(@RequestBody Flowable<Person> flowable) {
			return flowable.toList().doOnSuccess(persons::addAll).ignoreElement();
		}
	}


	@XmlRootElement
	@SuppressWarnings("unused")
	private static class Person {

		private String name;

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
		public boolean equals(@Nullable Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Person person = (Person) o;
			return Objects.equals(this.name, person.name);
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


	private interface Controller23791<E> {

		@PostMapping("/23791")
		default Mono<String> test(@RequestBody Mono<E> body) {
			return body.map(value -> value.getClass().getSimpleName());
		}
	}

	@RestController
	private static class ConcreteController23791 implements Controller23791<Person> {
	}

}
