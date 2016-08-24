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
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.tests.TestSubscriber;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.reactive.WebClient;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.WebReactiveConfiguration;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.springframework.web.client.reactive.ClientWebRequestBuilders.get;
import static org.springframework.web.client.reactive.ResponseExtractors.bodyStream;

/**
 * @author Sebastien Deleuze
 */
public class SseIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private AnnotationConfigApplicationContext wac;

	private WebClient webClient;


	@Before
	public void setup() throws Exception {
		super.setup();
		this.webClient = new WebClient(new ReactorClientHttpConnector());
	}


	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext();
		this.wac.register(TestConfiguration.class);
		this.wac.refresh();

		DispatcherHandler webHandler = new DispatcherHandler();
		webHandler.setApplicationContext(this.wac);

		return WebHttpHandlerBuilder.webHandler(webHandler).build();
	}

	@Test
	public void sseAsString() throws Exception {
		Flux<String> result = this.webClient
				.perform(get("http://localhost:" + port + "/sse/string")
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
				.perform(get("http://localhost:" + port + "/sse/person")
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
				.perform(get("http://localhost:" + port + "/sse/event")
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
	@SuppressWarnings("unused")
	static class TestConfiguration extends WebReactiveConfiguration {

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
