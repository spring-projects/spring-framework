/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.http.MediaType.*;

/**
 * @author Sebastien Deleuze
 */
public class SseIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private AnnotationConfigApplicationContext wac;

	private WebClient webClient;


	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create("http://localhost:" + this.port + "/sse");
	}


	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext();
		this.wac.register(TestConfiguration.class);
		this.wac.refresh();

		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(this.wac)).build();
	}

	@Test
	public void sseAsString() {
		Flux<String> result = this.webClient.get()
				.uri("/string")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.expectNext("foo 0")
				.expectNext("foo 1")
				.thenCancel()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsPerson() {
		Flux<Person> result = this.webClient.get()
				.uri("/person")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(Person.class);

		StepVerifier.create(result)
				.expectNext(new Person("foo 0"))
				.expectNext(new Person("foo 1"))
				.thenCancel()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsEvent() {

		Assume.assumeTrue(server instanceof JettyHttpServer);

		Flux<ServerSentEvent<Person>> result = this.webClient.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Person>>() {});

		verifyPersonEvents(result);
	}

	@Test
	public void sseAsEventWithoutAcceptHeader() {
		Flux<ServerSentEvent<Person>> result = this.webClient.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Person>>() {});

		verifyPersonEvents(result);
	}

	private void verifyPersonEvents(Flux<ServerSentEvent<Person>> result) {
		StepVerifier.create(result)
				.consumeNextWith( event -> {
					assertEquals("0", event.id());
					assertEquals(new Person("foo 0"), event.data());
					assertEquals("bar 0", event.comment());
					assertNull(event.event());
					assertNull(event.retry());
				})
				.consumeNextWith( event -> {
					assertEquals("1", event.id());
					assertEquals(new Person("foo 1"), event.data());
					assertEquals("bar 1", event.comment());
					assertNull(event.event());
					assertNull(event.retry());
				})
				.thenCancel()
				.verify(Duration.ofSeconds(5L));
	}

	@Test // SPR-16494
	@Ignore // https://github.com/reactor/reactor-netty/issues/283
	public void serverDetectsClientDisconnect() {

		assumeTrue(this.server instanceof ReactorHttpServer);

		Flux<String> result = this.webClient.get()
				.uri("/infinite")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(String.class);

		StepVerifier.create(result)
				.expectNext("foo 0")
				.expectNext("foo 1")
				.thenCancel()
				.verify(Duration.ofSeconds(5L));

		SseController controller = this.wac.getBean(SseController.class);
		controller.cancellation.block(Duration.ofSeconds(5));
	}


	@RestController
	@SuppressWarnings("unused")
	@RequestMapping("/sse")
	static class SseController {

		private static final Flux<Long> INTERVAL = interval(Duration.ofMillis(100), 50);

		private MonoProcessor<Void> cancellation = MonoProcessor.create();


		@GetMapping("/string")
		Flux<String> string() {
			return INTERVAL.map(l -> "foo " + l);
		}

		@GetMapping("/person")
		Flux<Person> person() {
			return INTERVAL.map(l -> new Person("foo " + l));
		}

		@GetMapping("/event")
		Flux<ServerSentEvent<Person>> sse() {
			return INTERVAL.take(2).map(l ->
					ServerSentEvent.builder(new Person("foo " + l))
							.id(Long.toString(l))
							.comment("bar " + l)
							.build());
		}

		@GetMapping("/infinite")
		Flux<String> infinite() {
			return Flux.just(0, 1).map(l -> "foo " + l)
					.mergeWith(Flux.never())
					.doOnCancel(() -> cancellation.onComplete());
		}
	}


	@Configuration
	@EnableWebFlux
	@SuppressWarnings("unused")
	static class TestConfiguration {

		@Bean
		public SseController sseController() {
			return new SseController();
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
			return "Person{name='" + this.name + '\'' + '}';
		}
	}

}
