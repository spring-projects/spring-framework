/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Tests with asynchronous request handling.
 *
 * @author Rossen Stoyanchev
 */
public class AsyncTests {

	private MockMvc mockMvc;

	private AsyncController asyncController;


	@Before
	public void setup() {
		this.asyncController = new AsyncController();
		this.mockMvc = standaloneSetup(this.asyncController).build();
	}

	@Test
	@Ignore
	public void testCallable() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("callable", "true"))
			.andDo(print())
			.andExpect(request().asyncStarted())
			.andExpect(request().asyncResult(new Person("Joe")))
			.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}

	@Test
	@Ignore
	public void testDeferredResult() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResult", "true"))
			.andExpect(request().asyncStarted())
			.andReturn();

		this.asyncController.onMessage("Joe");

		this.mockMvc.perform(asyncDispatch(mvcResult))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}

	@Test
	@Ignore
	public void testDeferredResultWithSetValue() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResultWithSetValue", "true"))
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(new Person("Joe")))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}


	@Controller
	private static class AsyncController {

		private Collection<DeferredResult<Person>> deferredResults = new CopyOnWriteArrayList<DeferredResult<Person>>();


		@RequestMapping(value="/{id}", params="callable", produces="application/json")
		@ResponseBody
		public Callable<Person> getCallable(final Model model) {
			return new Callable<Person>() {
				@Override
				public Person call() throws Exception {
					return new Person("Joe");
				}
			};
		}

		@RequestMapping(value="/{id}", params="deferredResult", produces="application/json")
		@ResponseBody
		public DeferredResult<Person> getDeferredResult() {
			DeferredResult<Person> deferredResult = new DeferredResult<Person>();
			this.deferredResults.add(deferredResult);
			return deferredResult;
		}

		@RequestMapping(value="/{id}", params="deferredResultWithSetValue", produces="application/json")
		@ResponseBody
		public DeferredResult<Person> getDeferredResultWithSetValue() {
			DeferredResult<Person> deferredResult = new DeferredResult<Person>();
			deferredResult.setResult(new Person("Joe"));
			return deferredResult;
		}

		public void onMessage(String name) {
			for (DeferredResult<Person> deferredResult : this.deferredResults) {
				deferredResult.setResult(new Person(name));
				this.deferredResults.remove(deferredResult);
			}
		}
	}

}
