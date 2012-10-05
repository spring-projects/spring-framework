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
package org.springframework.test.web.mock.servlet.samples.standalone;

import static org.springframework.test.web.mock.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.mock.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.mock.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.mock.Person;
import org.springframework.test.web.mock.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Tests with asynchronous request handling.
 *
 * @author Rossen Stoyanchev
 */
public class AsyncTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new AsyncController()).build();
	}

	@Test
	public void testDeferredResult() throws Exception {
		this.mockMvc.perform(get("/1").param("deferredResult", "true"))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted());
	}

	@Test
	public void testCallable() throws Exception {
		this.mockMvc.perform(get("/1").param("callable", "true"))
			.andExpect(status().isOk())
			.andExpect(request().asyncStarted())
			.andExpect(request().asyncResult(new Person("Joe")));
	}


	@Controller
	private static class AsyncController {

		@RequestMapping(value="/{id}", params="deferredResult", produces="application/json")
		public DeferredResult<Person> getDeferredResult() {
			return new DeferredResult<Person>();
		}

		@RequestMapping(value="/{id}", params="callable", produces="application/json")
		public Callable<Person> getCallable() {
			return new Callable<Person>() {
				public Person call() throws Exception {
					Thread.sleep(100);
					return new Person("Joe");
				}
			};
		}

	}

}
