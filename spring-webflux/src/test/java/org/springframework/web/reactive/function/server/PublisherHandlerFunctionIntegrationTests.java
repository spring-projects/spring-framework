/*
 * Copyright 2002-2017 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import java.util.List;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Arjen Poutsma
 * @author Jonathan Borenstein
 */
public class PublisherHandlerFunctionIntegrationTests
extends AbstractRouterFunctionIntegrationTests {

	private WebClient webClient = WebClient.create();


	@Override
	protected RouterFunction<?> routerFunction() {
		PersonHandler personHandler = new PersonHandler();
		return route(GET("/mono"), personHandler::mono)
				.and(route(POST("/mono"), personHandler::postMono))
				.and(route(GET("/flux"), personHandler::flux));
	}


	@Test
	public void mono() throws Exception {

		ClientResponse response = webClient.get()
				.uri("http://localhost:" + port + "/mono")
				.exchange()
				.block();

		Mono<Person> person = response.bodyToMono(Person.class);

		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals("John", person.block().getName());
	}

	@Test
	public void flux() throws Exception {

		ClientResponse response = webClient.get()
				.uri("http://localhost:" + port + "/flux")
				.exchange()
				.block();

		assertEquals(HttpStatus.OK, response.statusCode());

		Flux<List<Person>> fluxList = response.bodyToFlux(Person.class).buffer();
		List<Person> body = fluxList.blockFirst();

		assertEquals(2, body.size());
		assertEquals("John", body.get(0).getName());
		assertEquals("Jane", body.get(1).getName());
	}

	@Test
	public void postMono() {
		Person person = new Person("Jack");

		ClientResponse response = webClient.post()
				.uri("http://localhost:" + port + "/mono")
				.exchange(BodyInserters.fromObject(person)).block();

		Mono<Person> personResponse = response.bodyToMono(Person.class);


		assertEquals(HttpStatus.OK, response.statusCode());
		assertEquals("Jack", personResponse.block().getName());
	}


	private static class PersonHandler {

		public Mono<ServerResponse> mono(ServerRequest request) {
			Person person = new Person("John");		
			return ServerResponse.ok().body(fromPublisher(Mono.just(person), Person.class));
		}

		public Mono<ServerResponse> postMono(ServerRequest request) {
			Mono<Person> personMono = request.body(toMono(Person.class));
			return ServerResponse.ok().body(fromPublisher(personMono, Person.class));
		}

		public Mono<ServerResponse> flux(ServerRequest request) {
			Person person1 = new Person("John");
			Person person2 = new Person("Jane");
			return ServerResponse.ok().body(
					fromPublisher(Flux.just(person1, person2), Person.class));
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
