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

package org.springframework.web.reactive.function.server;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.BodyExtractors.toFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ResolvableType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.web.reactive.function.BodyInserters.fromServerSentEvents;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
public class SseHandlerFunctionIntegrationTests extends AbstractRouterFunctionIntegrationTests {


	private WebClient webClient;

	@Before
	public void createWebClient() {
		this.webClient = WebClient.create(new ReactorClientHttpConnector());
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
		ClientRequest<Void> request = ClientRequest
						.GET("http://localhost:{port}/string", this.port)
						.accept(TEXT_EVENT_STREAM)
						.build();

		Flux<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(toFlux(String.class)));

		StepVerifier.create(result)
				.expectNext("foo 0")
				.expectNext("foo 1")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}
	@Test
	public void sseAsPerson() throws Exception {
		ClientRequest<Void> request =
				ClientRequest
						.GET("http://localhost:{port}/person", this.port)
						.accept(TEXT_EVENT_STREAM)
						.build();

		Flux<Person> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(toFlux(Person.class)));

		StepVerifier.create(result)
				.expectNext(new Person("foo 0"))
				.expectNext(new Person("foo 1"))
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsEvent() throws Exception {
		ClientRequest<Void> request =
				ClientRequest
						.GET("http://localhost:{port}/event", this.port)
						.accept(TEXT_EVENT_STREAM)
						.build();

		ResolvableType type = ResolvableType.forClassWithGenerics(ServerSentEvent.class, String.class);
		Flux<ServerSentEvent<String>> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(toFlux(type)));

		StepVerifier.create(result)
				.consumeNextWith( event -> {
					assertEquals("0", event.id().get());
					assertEquals("foo", event.data().get());
					assertEquals("bar", event.comment().get());
					assertFalse(event.event().isPresent());
					assertFalse(event.retry().isPresent());
				})
				.consumeNextWith( event -> {
					assertEquals("1", event.id().get());
					assertEquals("foo", event.data().get());
					assertEquals("bar", event.comment().get());
					assertFalse(event.event().isPresent());
					assertFalse(event.retry().isPresent());
				})
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	private static class SseHandler {

		public Mono<ServerResponse> string(ServerRequest request) {
			Flux<String> flux = Flux.interval(Duration.ofMillis(100)).map(l -> "foo " + l).take(2);
			return ServerResponse.ok().body(fromServerSentEvents(flux, String.class));
		}

		public Mono<ServerResponse> person(ServerRequest request) {
			Flux<Person> flux = Flux.interval(Duration.ofMillis(100))
					.map(l -> new Person("foo " + l)).take(2);
			return ServerResponse.ok().body(fromServerSentEvents(flux, Person.class));
		}

		public Mono<ServerResponse> sse(ServerRequest request) {
			Flux<ServerSentEvent<String>> flux = Flux.interval(Duration.ofMillis(100))
					.map(l -> ServerSentEvent.<String>builder().data("foo")
							.id(Long.toString(l))
							.comment("bar")
							.build()).take(2);

			return ServerResponse.ok().body(fromServerSentEvents(flux));
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
