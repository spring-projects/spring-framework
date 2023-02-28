/*
 * Copyright 2002-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Response written from {@code @ResponseBody} method.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class ResponseBodyTests {

	@Test
	void json() throws Exception {
		standaloneSetup(new PersonController()).defaultResponseCharacterEncoding(UTF_8).build()
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


	@RestController
	private static class PersonController {

		@GetMapping("/person/{name}")
		Person get(@PathVariable String name) {
			Person person = new Person(name);
			person.setAge(42);
			return person;
		}
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
