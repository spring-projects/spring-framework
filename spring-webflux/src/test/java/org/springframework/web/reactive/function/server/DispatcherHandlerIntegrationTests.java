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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.AbstractHttpHandlerIntegrationTests;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.reactive.function.server.support.HandlerFunctionAdapter;
import org.springframework.web.reactive.function.server.support.ServerResponseResultHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.junit.Assert.assertEquals;
import static org.springframework.web.reactive.function.BodyInserters.fromObject;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * Tests the use of {@link HandlerFunction} and {@link RouterFunction} in a
 * {@link DispatcherHandler}.
 * @author Arjen Poutsma
 */
public class DispatcherHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {
	
	private AnnotationConfigApplicationContext wac;

	private RestTemplate restTemplate;

	@Before
	public void createRestTemplate() {
		this.restTemplate = new RestTemplate();
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
	public void mono() throws Exception {
		ResponseEntity<Person> result =
				this.restTemplate.getForEntity("http://localhost:" + this.port + "/mono", Person.class);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		assertEquals("John", result.getBody().getName());
	}

	@Test
	public void flux() throws Exception {
		ParameterizedTypeReference<List<Person>> reference = new ParameterizedTypeReference<List<Person>>() {};
		ResponseEntity<List<Person>> result =
				this.restTemplate
						.exchange("http://localhost:" + this.port + "/flux", HttpMethod.GET, null, reference);

		assertEquals(HttpStatus.OK, result.getStatusCode());
		List<Person> body = result.getBody();
		assertEquals(2, body.size());
		assertEquals("John", body.get(0).getName());
		assertEquals("Jane", body.get(1).getName());
	}
	

	@Configuration
	static class TestConfiguration extends WebFluxConfigurationSupport {

		@Bean
		public PersonHandler personHandler() {
			return new PersonHandler();
		}

		@Bean
		public HandlerAdapter handlerAdapter() {
			return new HandlerFunctionAdapter();
		}

		@Bean
		public HandlerMapping handlerMapping(RouterFunction<?> routerFunction,
				ApplicationContext applicationContext) {
			return RouterFunctions.toHandlerMapping(routerFunction,
					new HandlerStrategies() {
						@Override
						public Supplier<Stream<HttpMessageReader<?>>> messageReaders() {
							return () -> getMessageReaders().stream();
						}

						@Override
						public Supplier<Stream<HttpMessageWriter<?>>> messageWriters() {
							return () -> getMessageWriters().stream();
						}

						@Override
						public Supplier<Stream<ViewResolver>> viewResolvers() {
							return Stream::empty;
						}

						@Override
						public Function<ServerRequest, Optional<Locale>> localeResolver() {
							return DefaultHandlerStrategiesBuilder.DEFAULT_LOCALE_RESOLVER;
						}
					});
		}

		@Bean
		public RouterFunction<?> routerFunction() {
			PersonHandler personHandler = personHandler();
			return route(RequestPredicates.GET("/mono"), personHandler::mono)
					.and(route(RequestPredicates.GET("/flux"), personHandler::flux));
		}

		@Bean
		public ServerResponseResultHandler responseResultHandler() {
			return new ServerResponseResultHandler();
		}
	}
	
	private static class PersonHandler {

		public Mono<ServerResponse> mono(ServerRequest request) {
			Person person = new Person("John");
			return ServerResponse.ok().body(fromObject(person));
		}

		public Mono<ServerResponse> flux(ServerRequest request) {
			Person person1 = new Person("John");
			Person person2 = new Person("Jane");
			return ServerResponse.ok().body(
					fromPublisher(Flux.just(person1, person2), Person.class));
		}

		public Mono<ServerResponse> view() {
			return ServerResponse.ok().render("foo", "bar");
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
			return this.name;
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
			Person
					person = (Person) o;
			return !(this.name != null ? !this.name.equals(person.name) : person.name != null);
		}

		@Override
		public int hashCode() {
			return this.name != null ? this.name.hashCode() : 0;
		}

		@Override
		public String toString() {
			return "Person{" +
					"name='" + this.name + '\'' +
					'}';
		}
	}
	
	
}
