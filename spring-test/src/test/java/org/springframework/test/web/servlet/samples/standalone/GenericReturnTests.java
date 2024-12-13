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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Demonstrates, if returning a List&gt;? extends SomeType&lt; can be correctly serialized.
 *
 * @author Roland Praml
 */
public class GenericReturnTests {
	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	@JsonSubTypes({
			@JsonSubTypes.Type(value = SubType1.class, name = "one"),
			@JsonSubTypes.Type(value = SubType2.class, name = "two")
	})
	public static class BaseType {

	}

	public static class SubType1 extends BaseType {
	}

	public static class SubType2 extends BaseType {
	}

	@Test
	public void genericReturnTest() throws Exception {

		standaloneSetup(new Controller()).build()
				.perform(get("/genericReturnList").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$[0].type").value("one"))
				.andExpect(jsonPath("$[1].type").value("two"));
	}

	@RestController
	@SuppressWarnings("unchecked")
	public static class Controller {

		@GetMapping(value = "/genericReturnList", produces = MediaType.APPLICATION_JSON_VALUE)
		public <T extends BaseType> List<T> get() {
			List<T> list = new ArrayList<>();
			list.add((T) new SubType1());
			list.add((T) new SubType2());
			return list;
		}

	}
}
