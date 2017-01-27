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

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebReactive;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientOperations;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.springframework.core.ResolvableType.forClassWithGenerics;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.web.reactive.function.BodyExtractors.toFlux;


/**
 * @author Sebastien Deleuze
 */
public class SseIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private AnnotationConfigApplicationContext wac;

	private WebClientOperations operations;


	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		WebClient client = WebClient.create(new ReactorClientHttpConnector());
		UriBuilderFactory factory = new DefaultUriBuilderFactory("http://localhost:" + this.port + "/sse");
		this.operations = WebClientOperations.builder(client).uriBuilderFactory(factory).build();
	}


	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext();
		this.wac.register(TestConfiguration.class);
		this.wac.refresh();

		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(this.wac)).build();
	}

	@Test
	public void sseAsString() throws Exception {
		Flux<String> result = this.operations.get()
				.uri("/string")
				.accept(TEXT_EVENT_STREAM)
				.exchange()
				.flatMap(response -> response.bodyToFlux(String.class));

		StepVerifier.create(result)
				.expectNext("foo 0")
				.expectNext("foo 1")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}
	@Test
	public void sseAsPerson() throws Exception {
		Flux<Person> result = this.operations.get()
				.uri("/person")
				.accept(TEXT_EVENT_STREAM)
				.exchange()
				.flatMap(response -> response.bodyToFlux(Person.class));

		StepVerifier.create(result)
				.expectNext(new Person("foo 0"))
				.expectNext(new Person("foo 1"))
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsEvent() throws Exception {
		ResolvableType type = forClassWithGenerics(ServerSentEvent.class, String.class);
		Flux<ServerSentEvent<String>> result = this.operations.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.exchange()
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

	@Test
	public void sseAsEventWithoutAcceptHeader() throws Exception {
		Flux<ServerSentEvent<String>> result = this.operations.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.exchange()
				.flatMap(response -> response.body(toFlux(
						forClassWithGenerics(ServerSentEvent.class, String.class))));

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

	@RestController
	@SuppressWarnings("unused")
	static class SseController {

		@RequestMapping("/sse/string")
		Flux<String> string() {
			return Flux.interval(Duration.ofMillis(100)).map(l -> "foo " + l).take(2);
		}

		@RequestMapping("/sse/person")
		Flux<Person> person() {
			return Flux.interval(Duration.ofMillis(100)).map(l -> new Person("foo " + l)).take(2);
		}

		@RequestMapping("/sse/event")
		Flux<ServerSentEvent<String>> sse() {
			return Flux.interval(Duration.ofMillis(100)).map(l -> ServerSentEvent.builder("foo")
					.id(Long.toString(l))
					.comment("bar")
					.build()).take(2);
		}

	}

	@Configuration
	@EnableWebReactive
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		public SseController sseController() {
			return new SseController();
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
