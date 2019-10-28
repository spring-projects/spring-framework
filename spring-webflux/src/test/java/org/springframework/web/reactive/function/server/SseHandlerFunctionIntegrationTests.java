/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.time.Duration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.BodyInserters.fromServerSentEvents;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 * @author Sam Brannen
 */
class SseHandlerFunctionIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private WebClient webClient;


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}

	@Override
	protected RouterFunction<?> routerFunction() {
		SseHandler sseHandler = new SseHandler();
		return route(RequestPredicates.GET("/string"), sseHandler::string)
				.and(route(RequestPredicates.GET("/person"), sseHandler::person))
				.and(route(RequestPredicates.GET("/event"), sseHandler::sse));
	}


	@ParameterizedHttpServerTest
	void sseAsString(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Flux<String> result = this.webClient.get()
				.uri("/string")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.expectNext("foo 0")
				.expectNext("foo 1")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@ParameterizedHttpServerTest
	void sseAsPerson(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Flux<Person> result = this.webClient.get()
				.uri("/person")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(Person.class);

		StepVerifier.create(result)
				.expectNext(new Person("foo 0"))
				.expectNext(new Person("foo 1"))
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@ParameterizedHttpServerTest
	void sseAsEvent(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		Flux<ServerSentEvent<String>> result = this.webClient.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {});

		StepVerifier.create(result)
				.consumeNextWith( event -> {
					assertThat(event.id()).isEqualTo("0");
					assertThat(event.data()).isEqualTo("foo");
					assertThat(event.comment()).isEqualTo("bar");
					assertThat(event.event()).isNull();
					assertThat(event.retry()).isNull();
				})
				.consumeNextWith( event -> {
					assertThat(event.id()).isEqualTo("1");
					assertThat(event.data()).isEqualTo("foo");
					assertThat(event.comment()).isEqualTo("bar");
					assertThat(event.event()).isNull();
					assertThat(event.retry()).isNull();
				})
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}


	private static class SseHandler {

		private static final Flux<Long> INTERVAL = testInterval(Duration.ofMillis(100), 2);

		Mono<ServerResponse> string(ServerRequest request) {
			return ServerResponse.ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(INTERVAL.map(aLong -> "foo " + aLong), String.class);
		}

		Mono<ServerResponse> person(ServerRequest request) {
			return ServerResponse.ok()
					.contentType(MediaType.TEXT_EVENT_STREAM)
					.body(INTERVAL.map(aLong -> new Person("foo " + aLong)), Person.class);
		}

		Mono<ServerResponse> sse(ServerRequest request) {
			Flux<ServerSentEvent<String>> body = INTERVAL
					.map(aLong -> ServerSentEvent.builder("foo").id("" + aLong).comment("bar").build());
			return ServerResponse.ok().body(fromServerSentEvents(body));
		}
	}


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
			return "Person{" + "name='" + name + '\'' + '}';
		}
	}

}
