/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

/**
 * MockMvcTestClient equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.RouterFunctionTests}.
 *
 * @author Arjen Poutsma
 */
public class RouterFunctionTests {

	@Test
	void json() {
		execute("/person/Lee", body -> body.jsonPath("$.name").isEqualTo("Lee")
				.jsonPath("$.age").isEqualTo(42)
				.jsonPath("$.age").value(equalTo(42))
				.jsonPath("$.age").value(Float.class, equalTo(42.0f)));
	}

	@Test
	public void queryParameter() {
		execute("/search?name=George", body -> body.jsonPath("$.name").isEqualTo("George"));
	}


	@Nested
	class AsyncTests {

		@Test
		void completableFuture() {
			execute("/async/completableFuture", body -> body.json("{\"name\":\"Joe\",\"age\":0}"));
		}

		@Test
		void publisher() {
			execute("/async/publisher", body -> body.json("{\"name\":\"Joe\",\"age\":0}"));
		}

	}


	private void execute(String uri, Consumer<WebTestClient.BodyContentSpec> assertions) {
		RouterFunction<?> testRoute = testRoute();
		assertions.accept(MockMvcWebTestClient.bindToRouterFunction(testRoute).build()
				.get()
				.uri(uri)
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody());
	}

	private static RouterFunction<?> testRoute() {
		return route()
				.GET("/person/{name}", request -> {
					Person person = new Person(request.pathVariable("name"));
					person.setAge(42);
					return ok().body(person);
				})
				.GET("/search", request -> {
					String name = request.param("name").orElseThrow(NullPointerException::new);
					Person person = new Person(name);
					return ok().body(person);
				})
				.path("/async", b -> b
								.GET("/completableFuture", request -> {
									CompletableFuture<Person> future = new CompletableFuture<>();
									future.complete(new Person("Joe"));
									return ok().body(future);
								})
								.GET("/publisher", request -> {
									Mono<Person> mono = Mono.just(new Person("Joe"));
									return ok().body(mono);
								})
						)
				.route(RequestPredicates.all(), request -> ServerResponse.notFound().build())
				.build();
	}

	@SuppressWarnings("unused")
	private static class Person {

		private final String name;

		private int age;

		public Person(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}
	}
}
