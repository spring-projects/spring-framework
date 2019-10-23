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

package org.springframework.web.reactive.result.method.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.MonoProcessor;
import reactor.test.StepVerifier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.http.server.reactive.bootstrap.JettyHttpServer;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.http.server.reactive.bootstrap.TomcatHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

/**
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
class SseIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] server [{0}], webClient [{1}]")
	@MethodSource("arguments")
	protected @interface ParameterizedSseTest {
	}

	static Object[][] arguments() {
		return new Object[][] {
			{new JettyHttpServer(), new ReactorClientHttpConnector()},
			{new JettyHttpServer(), new JettyClientHttpConnector()},
			{new ReactorHttpServer(), new ReactorClientHttpConnector()},
			{new ReactorHttpServer(), new JettyClientHttpConnector()},
			{new TomcatHttpServer(), new ReactorClientHttpConnector()},
			{new TomcatHttpServer(), new JettyClientHttpConnector()},
			{new UndertowHttpServer(), new ReactorClientHttpConnector()},
			{new UndertowHttpServer(), new JettyClientHttpConnector()}
		};
	}


	private AnnotationConfigApplicationContext wac;

	private WebClient webClient;


	private void startServer(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		super.startServer(httpServer);

		this.webClient = WebClient
				.builder()
				.clientConnector(connector)
				.baseUrl("http://localhost:" + this.port + "/sse")
				.build();
	}

	@Override
	protected HttpHandler createHttpHandler() {
		this.wac = new AnnotationConfigApplicationContext(TestConfiguration.class);

		return WebHttpHandlerBuilder.webHandler(new DispatcherHandler(this.wac)).build();
	}

	@ParameterizedSseTest
	void sseAsString(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		startServer(httpServer, connector);

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

	@ParameterizedSseTest
	void sseAsPerson(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		startServer(httpServer, connector);

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

	@ParameterizedSseTest
	void sseAsEvent(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		assumeTrue(httpServer instanceof JettyHttpServer);

		startServer(httpServer, connector);

		Flux<ServerSentEvent<Person>> result = this.webClient.get()
				.uri("/event")
				.accept(TEXT_EVENT_STREAM)
				.retrieve()
				.bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Person>>() {});

		verifyPersonEvents(result);
	}

	@ParameterizedSseTest
	void sseAsEventWithoutAcceptHeader(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		startServer(httpServer, connector);

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
					assertThat(event.id()).isEqualTo("0");
					assertThat(event.data()).isEqualTo(new Person("foo 0"));
					assertThat(event.comment()).isEqualTo("bar 0");
					assertThat(event.event()).isNull();
					assertThat(event.retry()).isNull();
				})
				.consumeNextWith( event -> {
					assertThat(event.id()).isEqualTo("1");
					assertThat(event.data()).isEqualTo(new Person("foo 1"));
					assertThat(event.comment()).isEqualTo("bar 1");
					assertThat(event.event()).isNull();
					assertThat(event.retry()).isNull();
				})
				.thenCancel()
				.verify(Duration.ofSeconds(5L));
	}

	@ParameterizedSseTest // SPR-16494
	@Disabled // https://github.com/reactor/reactor-netty/issues/283
	void serverDetectsClientDisconnect(HttpServer httpServer, ClientHttpConnector connector) throws Exception {
		assumeTrue(httpServer instanceof ReactorHttpServer);

		startServer(httpServer, connector);

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

		private static final Flux<Long> INTERVAL = testInterval(Duration.ofMillis(100), 50);

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
