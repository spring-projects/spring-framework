/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.reactive.web.dispatch.method.annotation;


import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Stream;
import reactor.rx.Streams;
import rx.Observable;
import rx.Single;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.reactive.codec.encoder.JacksonJsonEncoder;
import org.springframework.reactive.codec.encoder.JsonObjectEncoder;
import org.springframework.reactive.codec.encoder.StringEncoder;
import org.springframework.reactive.web.dispatch.DispatcherHandler;
import org.springframework.reactive.web.http.AbstractHttpHandlerIntegrationTests;
import org.springframework.reactive.web.http.HttpHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
public class RequestMappingIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Override
	protected HttpHandler createHttpHandler() {

		StaticWebApplicationContext wac = new StaticWebApplicationContext();
		wac.registerSingleton("handlerMapping", RequestMappingHandlerMapping.class);
		wac.registerSingleton("handlerAdapter", RequestMappingHandlerAdapter.class);
		wac.getDefaultListableBeanFactory().registerSingleton("responseBodyResultHandler",
				new ResponseBodyResultHandler(Arrays.asList(new StringEncoder(), new JacksonJsonEncoder()), Arrays.asList(new JsonObjectEncoder())));
		wac.registerSingleton("controller", TestController.class);
		wac.refresh();

		DispatcherHandler dispatcherHandler = new DispatcherHandler();
		dispatcherHandler.setApplicationContext(wac);
		return dispatcherHandler;
	}

	@Test
	public void helloWithQueryParam() throws Exception {

		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI("http://localhost:" + port + "/param?name=George");
		RequestEntity<Void> request = RequestEntity.get(url).build();
		ResponseEntity<String> response = restTemplate.exchange(request, String.class);

		assertEquals("Hello George!", response.getBody());
	}

	@Test
	public void serializeAsPojo() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/person");
	}

	@Test
	public void serializeAsCompletableFuture() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/completable-future");
	}

	@Test
	public void serializeAsSingle() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/single");
	}

	@Test
	public void serializeAsPromise() throws Exception {
		serializeAsPojo("http://localhost:" + port + "/promise");
	}

	@Test
	public void serializeAsList() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/list");
	}

	@Test
	public void serializeAsPublisher() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/publisher");
	}

	@Test
	public void serializeAsObservable() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/observable");
	}

	@Test
	public void serializeAsReactorStream() throws Exception {
		serializeAsCollection("http://localhost:" + port + "/stream");
	}

	@Test
	public void publisherCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/publisher-capitalize");
	}

	@Test
	public void observableCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/observable-capitalize");
	}

	@Test
	public void streamCapitalize() throws Exception {
		capitalizeCollection("http://localhost:" + port + "/stream-capitalize");
	}

	@Test
	public void completableFutureCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/completable-future-capitalize");
	}

	@Test
	public void singleCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/single-capitalize");
	}

	@Test
	public void promiseCapitalize() throws Exception {
		capitalizePojo("http://localhost:" + port + "/promise-capitalize");
	}


	public void serializeAsPojo(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI(requestUrl);
		RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);

		assertEquals(new Person("Robert"), response.getBody());
	}

	public void serializeAsCollection(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI(requestUrl);
		RequestEntity<Void> request = RequestEntity.get(url).accept(MediaType.APPLICATION_JSON).build();
		List<Person> results = restTemplate.exchange(request, new ParameterizedTypeReference<List<Person>>(){}).getBody();

		assertEquals(2, results.size());
		assertEquals(new Person("Robert"), results.get(0));
		assertEquals(new Person("Marie"), results.get(1));
	}


	public void capitalizePojo(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI(requestUrl);
		RequestEntity<Person> request = RequestEntity
				.post(url)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(new Person("Robert"));
		ResponseEntity<Person> response = restTemplate.exchange(request, Person.class);

		assertEquals(new Person("ROBERT"), response.getBody());
	}


	public void capitalizeCollection(String requestUrl) throws Exception {
		RestTemplate restTemplate = new RestTemplate();

		URI url = new URI(requestUrl);
		List<Person> persons = Arrays.asList(new Person("Robert"), new Person("Marie"));
		RequestEntity<List<Person>> request = RequestEntity
				.post(url)
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON)
				.body(persons);
		List<Person> results = restTemplate.exchange(request, new ParameterizedTypeReference<List<Person>>(){}).getBody();

		assertEquals(2, results.size());
		assertEquals("ROBERT", results.get(0).getName());
		assertEquals("MARIE", results.get(1).getName());
	}


	@Controller
	@SuppressWarnings("unused")
	private static class TestController {

		@RequestMapping("/param")
		@ResponseBody
		public Publisher<String> handleWithParam(@RequestParam String name) {
			return Streams.just("Hello ", name, "!");
		}

		@RequestMapping("/person")
		@ResponseBody
		public Person personResponseBody() {
			return new Person("Robert");
		}

		@RequestMapping("/completable-future")
		@ResponseBody
		public CompletableFuture<Person> completableFutureResponseBody() {
			return CompletableFuture.completedFuture(new Person("Robert"));
		}

		@RequestMapping("/single")
		@ResponseBody
		public Single<Person> singleResponseBody() {
			return Single.just(new Person("Robert"));
		}

		@RequestMapping("/promise")
		@ResponseBody
		public Promise<Person> promiseResponseBody() {
			return Promises.success(new Person("Robert"));
		}

		@RequestMapping("/list")
		@ResponseBody
		public List<Person> listResponseBody() {
			return Arrays.asList(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/publisher")
		@ResponseBody
		public Publisher<Person> publisherResponseBody() {
			return Streams.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/observable")
		@ResponseBody
		public Observable<Person> observableResponseBody() {
			return Observable.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/stream")
		@ResponseBody
		public Stream<Person> reactorStreamResponseBody() {
			return Streams.just(new Person("Robert"), new Person("Marie"));
		}

		@RequestMapping("/publisher-capitalize")
		@ResponseBody
		public Publisher<Person> publisherCapitalize(@RequestBody Publisher<Person> persons) {
			return Streams.wrap(persons).map(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

		@RequestMapping("/observable-capitalize")
		@ResponseBody
		public Observable<Person> observableCapitalize(@RequestBody Observable<Person> persons) {
			return persons.map(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

		@RequestMapping("/stream-capitalize")
		@ResponseBody
		public Stream<Person> streamCapitalize(@RequestBody Stream<Person> persons) {
			return persons.map(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

		@RequestMapping("/completable-future-capitalize")
		@ResponseBody
		public CompletableFuture<Person> completableFutureCapitalize(@RequestBody CompletableFuture<Person> personFuture) {
			return personFuture.thenApply(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

		@RequestMapping("/single-capitalize")
		@ResponseBody
		public Single<Person> singleCapitalize(@RequestBody Single<Person> personFuture) {
			return personFuture.map(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

		@RequestMapping("/promise-capitalize")
		@ResponseBody
		public Promise<Person> promiseCapitalize(@RequestBody Promise<Person> personFuture) {
			return personFuture.map(person -> {
				person.setName(person.getName().toUpperCase());
				return person;
			});
		}

	}

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
			return name.equals(((Person)o).name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}
	}

}
