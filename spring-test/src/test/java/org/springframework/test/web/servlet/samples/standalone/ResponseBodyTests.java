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

package org.springframework.test.web.servlet.samples.standalone;

import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
public class ResponseBodyTests {

	@Test
	public void json() throws Exception {
		standaloneSetup(new PersonController()).build()
				.perform(get("/person/Lee").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.name").value("Lee"))
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
		public Person get(@PathVariable String name) {
			Person person = new Person(name);
			person.setAge(42);
			return person;
		}
	}

	@SuppressWarnings("unused")
	private static class Person {

		@NotNull
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
