/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

/**
 * Examples of expectations on response header values.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderAssertionTests {

	private MockMvc mockMvc;

	private PersonController personController;

	@Before
	public void setup() {
		this.personController = new PersonController();
		this.mockMvc = standaloneSetup(this.personController).build();
	}

	@Test
	public void testValue() throws Exception {
		long currentTime = new Date().getTime();
		this.personController.setStubTimestamp(currentTime);
		this.mockMvc.perform(get("/persons/1").header("If-Modified-Since", currentTime - (1000 * 60)))
			.andExpect(header().string("Last-Modified", String.valueOf(currentTime)));
	}

	@Test
	public void testLongValue() throws Exception {
		long currentTime = new Date().getTime();
		this.personController.setStubTimestamp(currentTime);
		this.mockMvc.perform(get("/persons/1").header("If-Modified-Since", currentTime - (1000 * 60)))
			.andExpect(header().longValue("Last-Modified", currentTime));
	}

	@Test
	public void testMatcher() throws Exception {
		long currentTime = new Date().getTime();
		this.personController.setStubTimestamp(currentTime);
		this.mockMvc.perform(get("/persons/1").header("If-Modified-Since", currentTime))
			.andExpect(status().isNotModified())
			.andExpect(header().string("Last-Modified", nullValue()));
	}


	@Controller
	private static class PersonController {

		private long timestamp;

		public void setStubTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		@RequestMapping("/persons/{id}")
		@ResponseBody
		public Person showEntity(@PathVariable long id, WebRequest request) {
			if (request.checkNotModified(calculateLastModified(id))) {
				return null;
			}
			return new Person("Jason");
		}

		private long calculateLastModified(long id) {
			return this.timestamp;
		}
	}
}
