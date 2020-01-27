/*
 * Copyright 2002-2020 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
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


	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		assertThat(performGet("/raw-response/publisher", JSON, Person.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/flux", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		ResponseEntity<String> responseEntity = performGet("/raw-response/mono", new HttpHeaders(), String.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(6);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/observable", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithRxJava2Observable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/rxjava2-observable",
				new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void byteBufferResponseBodyWithFlowable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		String expected = "Hello!";
		assertThat(performGet("/raw-response/flowable", new HttpHeaders(), String.class).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBody(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/person", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithCompletableFuture(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/completable-future", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> responseEntity = performGet("/person-response/mono", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(responseEntity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest // SPR-17506
	public void personResponseBodyWithEmptyMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Person> responseEntity = performGet("/person-response/mono-empty", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(0);
		assertThat(responseEntity.getBody()).isNull();

		// As we're on the same connection, the 2nd request proves server response handling
		// did complete after the 1st request..
		responseEntity = performGet("/person-response/mono-empty", JSON, Person.class);
		assertThat(responseEntity.getHeaders().getContentLength()).isEqualTo(0);
		assertThat(responseEntity.getBody()).isNull();
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithMonoDeclaredAsObject(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/mono-declared-as-object", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/single", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithMonoResponseEntity(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Person expected = new Person("Robert");
		ResponseEntity<Person> entity = performGet("/person-response/mono-response-entity", JSON, Person.class);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(17);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest // SPR-16172
	public void personResponseBodyWithMonoResponseEntityXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);


		String url = "/person-response/mono-response-entity-xml";
		ResponseEntity<String> entity = performGet(url, new HttpHeaders(), String.class);
		String actual = entity.getBody();

		assertThat(entity.getHeaders().getContentLength()).isEqualTo(91);
		assertThat(actual).isEqualTo(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
				"<person><name>Robert</name></person>"));
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithList(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		ResponseEntity<List<Person>> entity = performGet("/person-response/list", JSON, PERSON_LIST);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(36);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		ResponseEntity<List<Person>> entity = performGet("/person-response/publisher", JSON, PERSON_LIST);
		assertThat(entity.getHeaders().getContentLength()).isEqualTo(-1);
		assertThat(entity.getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertThat(performGet("/person-response/flux", JSON, PERSON_LIST).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void personResponseBodyWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> expected = asList(new Person("Robert"), new Person("Marie"));
		assertThat(performGet("/person-response/observable", JSON, PERSON_LIST).getBody()).isEqualTo(expected);
	}

	@ParameterizedHttpServerTest
	public void resource(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<byte[]> response = performGet("/resource", new HttpHeaders(), byte[].class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.hasBody()).isTrue();
		assertThat(response.getHeaders().getContentLength()).isEqualTo(951);
		assertThat(response.getBody().length).isEqualTo(951);
		assertThat(response.getHeaders().getContentType()).isEqualTo(new MediaType("image", "png"));
	}

	@ParameterizedHttpServerTest
	public void personTransform(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/person", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithCompletableFuture(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/completable-future", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/mono", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest  // SPR-16759
	public void personTransformWithMonoAndXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/mono", MediaType.APPLICATION_XML, new Person("Robert"),
				MediaType.APPLICATION_XML, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/single", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithRxJava2Single(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/rxjava2-single", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithRxJava2Maybe(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThat(performPost("/person-transform/rxjava2-maybe", JSON, new Person("Robert"),
				JSON, Person.class).getBody()).isEqualTo(new Person("ROBERT"));
	}

	@ParameterizedHttpServerTest
	public void personTransformWithPublisher(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/publisher", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	public void personTransformWithFlux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/flux", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	public void personTransformWithObservable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/observable", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	public void personTransformWithRxJava2Observable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/rxjava2-observable", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	public void personTransformWithFlowable(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		List<?> req = asList(new Person("Robert"), new Person("Marie"));
		List<?> res = asList(new Person("ROBERT"), new Person("MARIE"));
		assertThat(performPost("/person-transform/flowable", JSON, req, JSON, PERSON_LIST).getBody()).isEqualTo(res);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithPublisherJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/publisher", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithPublisherXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/publisher", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost(
				"/person-create/mono", JSON, new Person("Robert"), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(1);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithSingle(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost(
				"/person-create/single", JSON, new Person("Robert"), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(1);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithRxJava2Single(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost(
				"/person-create/rxjava2-single", JSON, new Person("Robert"), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(1);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithFluxJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/flux", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithFluxXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flux", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithObservableJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/observable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithRxJava2ObservableJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/rxjava2-observable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithObservableXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/observable", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithRxJava2ObservableXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		String url = "/person-create/rxjava2-observable";
		ResponseEntity<Void> response = performPost(url, APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithFlowableJson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Void> entity = performPost("/person-create/flowable", JSON,
				asList(new Person("Robert"), new Person("Marie")), null, Void.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest
	public void personCreateWithFlowableXml(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		People people = new People(new Person("Robert"), new Person("Marie"));
		ResponseEntity<Void> response = performPost("/person-create/flowable", APPLICATION_XML, people, null, Void.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(getApplicationContext().getBean(PersonCreateController.class).persons.size()).isEqualTo(2);
	}

	@ParameterizedHttpServerTest // gh-23791
	public void personCreateViaDefaultMethodWithGenerics(HttpServer httpServer) throws Exception {
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

		@GetMapping("/mono")
		public Mono<ByteBuffer> getMonoString() {
			return Mono.just(ByteBuffer.wrap("Hello!".getBytes()));
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

		@GetMapping("/mono-empty")
		public Mono<Person> getMonoEmpty() {
			return Mono.empty();
		}

		@GetMapping("/mono-declared-as-object")
		public Object getMonoDeclaredAsObject() {
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

		@GetMapping("/mono-response-entity-xml")
		public ResponseEntity<Mono<Person>> getMonoResponseEntityXml() {
			Mono<Person> body = Mono.just(new Person("Robert"));
			return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(body);
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
			return new ClassPathResource("/org/springframework/web/reactive/spring.png");
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
			return single.map(persons::add).ignoreElement();
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
		public io.reactivex.Completable createWithRxJava2Observable(
				@RequestBody io.reactivex.Observable<Person> observable) {

			return observable.toList().doOnSuccess(persons::addAll).ignoreElement();
		}

		@PostMapping("/flowable")
		public io.reactivex.Completable createWithFlowable(@RequestBody Flowable<Person> flowable) {
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
