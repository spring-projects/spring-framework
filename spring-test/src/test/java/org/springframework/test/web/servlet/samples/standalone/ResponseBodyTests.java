/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.samples.standalone;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Response written from {@code @ResponseBody} method.
 *
 * @author Rossen Stoyanchev
 */
public class ResponseBodyTests {

	@Test
	public void json() throws Exception {
		standaloneSetup(new PersonController()).build()
				.perform(get("/person/Lee").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json;charset=UTF-8"))
				.andExpect(jsonPath("$.name").value("Lee"));
	}


	@Controller
	private class PersonController {

		@RequestMapping(value="/person/{name}")
		@ResponseBody
		public Person get(@PathVariable String name) {
			return new Person(name);
		}
	}

}
