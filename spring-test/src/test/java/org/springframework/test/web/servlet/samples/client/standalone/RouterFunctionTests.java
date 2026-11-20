/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.samples.client.standalone;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.hamcrest.MatcherAssert;
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
class RouterFunctionTests {

	@Test
	void json() {
		execute("/person/Lee", body -> body.jsonPath("$.name").isEqualTo("Lee")
				.jsonPath("$.age").isEqualTo(42)
				.jsonPath("$.age").value(v -> MatcherAssert.assertThat(v, equalTo(42)))
				.jsonPath("$.age").value(Float.class, v -> MatcherAssert.assertThat(v, equalTo(42.0f))));
	}

	@Test
	void queryParameter() {
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
