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

package org.springframework.web.reactive.function;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.client.reactive.WebClient;

import static org.springframework.web.client.reactive.ClientWebRequestBuilders.get;
import static org.springframework.web.client.reactive.ResponseExtractors.bodyStream;
import static org.springframework.web.reactive.function.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
public class SseHandlerFunctionIntegrationTests
		extends AbstractRouterFunctionIntegrationTests {

	private WebClient webClient;

	@Before
	public void createWebClient() {
		this.webClient = new WebClient(new ReactorClientHttpConnector());
	}

	@Override
	protected RouterFunction<?> routerFunction() {
		SseHandler sseHandler = new SseHandler();
		return route(RequestPredicates.GET("/string"), sseHandler::string)
				.and(route(RequestPredicates.GET("/person"), sseHandler::person))
				.and(route(RequestPredicates.GET("/event"), sseHandler::sse));
	}


	@Test
	public void sseAsString() throws Exception {
		Flux<String> result = this.webClient
				.perform(get("http://localhost:" + port + "/string")
				.accept(new MediaType("text", "event-stream")))
				.extract(bodyStream(String.class))
				.filter(s -> !s.equals("\n"))
				.map(s -> (s.replace("\n", "")))
				.take(2);

		TestSubscriber
				.subscribe(result)
				.await(Duration.ofSeconds(5))
				.assertValues("data:foo 0", "data:foo 1");
	}

	@Test
	public void sseAsPerson() throws Exception {
		Mono<String> result = this.webClient
				.perform(get("http://localhost:" + port + "/person")
				.accept(new MediaType("text", "event-stream")))
				.extract(bodyStream(String.class))
				.filter(s -> !s.equals("\n"))
				.map(s -> s.replace("\n", ""))
				.takeUntil(s -> s.endsWith("foo 1\"}"))
				.reduce((s1, s2) -> s1 + s2);

		TestSubscriber
				.subscribe(result)
				.await(Duration.ofSeconds(5))
				.assertValues("data:{\"name\":\"foo 0\"}data:{\"name\":\"foo 1\"}");
	}

	@Test
	public void sseAsEvent() throws Exception {
		Flux<String> result = this.webClient
				.perform(get("http://localhost:" + port + "/event")
				.accept(new MediaType("text", "event-stream")))
				.extract(bodyStream(String.class))
				.filter(s -> !s.equals("\n"))
				.map(s -> s.replace("\n", ""))
				.take(2);

		TestSubscriber
				.subscribe(result)
				.await(Duration.ofSeconds(5))
				.assertValues(
						"id:0:bardata:foo",
						"id:1:bardata:foo"
				);
	}
	private static class SseHandler {

		public Response<Publisher<String>> string(Request request) {
			Flux<String> flux = Flux.interval(Duration.ofMillis(100)).map(l -> "foo " + l).take(2);
			return Response.ok().body(BodyPopulators.fromServerSentEvents(flux, String.class));
		}

		public Response<Publisher<Person>> person(Request request) {
			Flux<Person> flux = Flux.interval(Duration.ofMillis(100))
					.map(l -> new Person("foo " + l)).take(2);
			return Response.ok().body(BodyPopulators.fromServerSentEvents(flux, Person.class));
		}

		public Response<Publisher<ServerSentEvent<String>>> sse(Request request) {
			Flux<ServerSentEvent<String>> flux = Flux.interval(Duration.ofMillis(100))
					.map(l -> ServerSentEvent.<String>builder().data("foo")
							.id(Long.toString(l))
							.comment("bar")
							.build()).take(2);

			return Response.ok().body(BodyPopulators.fromServerSentEvents(flux));
		}
	}

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


}
