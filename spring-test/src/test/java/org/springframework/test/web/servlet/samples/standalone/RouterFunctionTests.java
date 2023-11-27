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

package org.springframework.test.web.servlet.samples.standalone;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.routerFunctions;
import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

/**
 * @author Arjen Poutsma
 */
public class RouterFunctionTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		RouterFunction<?> testRoute = testRoute();
		this.mockMvc = routerFunctions(testRoute).defaultResponseCharacterEncoding(UTF_8).build();
	}

	@Test
	void json() throws Exception {
		this.mockMvc
				// We use a name containing an umlaut to test UTF-8 encoding for the request and the response.
				.perform(get("/person/Jürgen").characterEncoding(UTF_8).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(content().encoding(UTF_8))
				.andExpect(content().string(containsString("Jürgen")))
				.andExpect(jsonPath("$.name").value("Jürgen"))
				.andExpect(jsonPath("$.age").value(42))
				.andExpect(jsonPath("$.age").value(42.0f))
				.andExpect(jsonPath("$.age").value(equalTo(42)))
				.andExpect(jsonPath("$.age").value(equalTo(42.0f), Float.class))
				.andExpect(jsonPath("$.age", equalTo(42)))
				.andExpect(jsonPath("$.age", equalTo(42.0f), Float.class));
	}

	@Test
	public void queryParameter() throws Exception {
		this.mockMvc
			.perform(get("/search?name=George").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.name").value("George"));
	}

	@Nested
	class AsyncTests {

		@Test
		void completableFuture() throws Exception {
			MvcResult mvcResult = mockMvc.perform(get("/async/completableFuture"))
					.andExpect(request().asyncStarted())
					.andReturn();

			mockMvc.perform(asyncDispatch(mvcResult))
					.andExpect(status().isOk())
					.andExpect(content().contentType(MediaType.APPLICATION_JSON))
					.andExpect(content().string("{\"name\":\"Joe\",\"age\":0}"));
		}

		@Test
		void publisher() throws Exception {
			MvcResult mvcResult = mockMvc.perform(get("/async/publisher"))
					.andExpect(request().asyncStarted())
					.andReturn();

			mockMvc.perform(asyncDispatch(mvcResult))
					.andExpect(status().isOk())
					.andExpect(content().contentType(MediaType.APPLICATION_JSON))
					.andExpect(content().string("{\"name\":\"Joe\",\"age\":0}"));

		}

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
