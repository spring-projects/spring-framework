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
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.BodyExtractors;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import reactor.test.StepVerifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.reactive.ClientRequest;
import org.springframework.web.client.reactive.WebClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebReactive;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;


/**
 * @author Sebastien Deleuze
 */
public class SseIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final MediaType EVENT_STREAM = new MediaType("text", "event-stream");


	private AnnotationConfigApplicationContext wac;

	private WebClient webClient;


	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = WebClient.create(new ReactorClientHttpConnector());
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
		ClientRequest<Void> request =
				ClientRequest
						.GET("http://localhost:{port}/sse/string", this.port)
						.accept(EVENT_STREAM)
						.build();

		Flux<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(BodyExtractors.toFlux(String.class)))
				.filter(s -> !s.equals("\n"))
				.map(s -> (s.replace("\n", "")))
				.take(2);

		StepVerifier.create(result)
				.expectNext("data:foo 0")
				.expectNext("data:foo 1")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}
	@Test
	public void sseAsPerson() throws Exception {
		ClientRequest<Void> request =
				ClientRequest
						.GET("http://localhost:{port}/sse/person", this.port)
						.accept(EVENT_STREAM)
						.build();

		Mono<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(BodyExtractors.toFlux(String.class)))
				.filter(s -> !s.equals("\n"))
				.map(s -> s.replace("\n", ""))
				.takeUntil(s -> s.endsWith("foo 1\"}"))
				.reduce((s1, s2) -> s1 + s2);

		StepVerifier.create(result)
				.expectNext("data:{\"name\":\"foo 0\"}data:{\"name\":\"foo 1\"}")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsEvent() throws Exception {
		ClientRequest<Void> request =
				ClientRequest
						.GET("http://localhost:{port}/sse/event", this.port)
						.accept(EVENT_STREAM)
						.build();
		Flux<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(BodyExtractors.toFlux(String.class)))
				.filter(s -> !s.equals("\n"))
				.map(s -> s.replace("\n", ""))
				.take(2);

		StepVerifier.create(result)
				.expectNext("id:0:bardata:foo")
				.expectNext("id:1:bardata:foo")
				.expectComplete()
				.verify(Duration.ofSeconds(5L));
	}

	@Test
	public void sseAsEventWithoutAcceptHeader() throws Exception {
		ClientRequest<Void> request =
		ClientRequest
				.GET("http://localhost:{port}/sse/event", this.port)
				.accept(EVENT_STREAM)
				.build();

		Flux<String> result = this.webClient
				.exchange(request)
				.flatMap(response -> response.body(BodyExtractors.toFlux(String.class)))
				.filter(s -> !s.equals("\n"))
				.map(s -> s.replace("\n", ""))
				.take(2);

		StepVerifier.create(result)
				.expectNext("id:0:bardata:foo")
				.expectNext("id:1:bardata:foo")
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
