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

import java.net.URI;
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.web.reactive.function.BodyExtractors.toMono;
import static org.springframework.web.reactive.function.BodyInserters.fromPublisher;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * @author Arjen Poutsma
 */
class PublisherHandlerFunctionIntegrationTests extends AbstractRouterFunctionIntegrationTests {

	private final RestTemplate restTemplate = new RestTemplate();


	@Override
	protected RouterFunction<?> routerFunction() {
		PersonHandler personHandler = new PersonHandler();
		return route(GET("/mono"), personHandler::mono)
				.and(route(POST("/mono"), personHandler::postMono))
				.and(route(GET("/flux"), personHandler::flux));
	}


	@ParameterizedHttpServerTest
	void mono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<Person> result =
				restTemplate.getForEntity("http://localhost:" + super.port + "/mono", Person.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getName()).isEqualTo("John");
	}

	@ParameterizedHttpServerTest
	void flux(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ParameterizedTypeReference<List<Person>> reference = new ParameterizedTypeReference<List<Person>>() {};
		ResponseEntity<List<Person>> result =
				restTemplate.exchange("http://localhost:" + super.port + "/flux", HttpMethod.GET, null, reference);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Person> body = result.getBody();
		assertThat(body.size()).isEqualTo(2);
		assertThat(body.get(0).getName()).isEqualTo("John");
		assertThat(body.get(1).getName()).isEqualTo("Jane");
	}

	@ParameterizedHttpServerTest
	void postMono(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		URI uri = URI.create("http://localhost:" + super.port + "/mono");
		Person person = new Person("Jack");
		RequestEntity<Person> requestEntity = RequestEntity.post(uri).body(person);
		ResponseEntity<Person> result = restTemplate.exchange(requestEntity, Person.class);

		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(result.getBody().getName()).isEqualTo("Jack");
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

		@SuppressWarnings("unused")
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
