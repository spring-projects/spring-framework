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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureTask;
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
	public void testCallable() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("callable", "true"))
				.andExpect(request().asyncStarted())
				.andExpect(request().asyncResult(new Person("Joe")))
				.andReturn();

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}

	@Test
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

	@Test
	public void testListenableFuture() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("listenableFuture", "true"))
				.andExpect(request().asyncStarted())
				.andReturn();

		this.asyncController.onMessage("Joe");

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}

	// SPR-12735

	@Test
	public void testPrintAsyncResult() throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(get("/1").param("deferredResult", "true"))
				.andDo(print())
				.andExpect(request().asyncStarted())
				.andReturn();

		this.asyncController.onMessage("Joe");

		this.mockMvc.perform(asyncDispatch(mvcResult))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(content().string("{\"name\":\"Joe\",\"someDouble\":0.0,\"someBoolean\":false}"));
	}



	@Controller
	private static class AsyncController {

		private Collection<DeferredResult<Person>> deferredResults =
				new CopyOnWriteArrayList<DeferredResult<Person>>();

		private Collection<ListenableFutureTask<Person>> futureTasks =
				new CopyOnWriteArrayList<ListenableFutureTask<Person>>();


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

		@RequestMapping(value="/{id}", params="listenableFuture", produces="application/json")
		@ResponseBody
		public ListenableFuture<Person> getListenableFuture() {
			ListenableFutureTask<Person> futureTask = new ListenableFutureTask<Person>(new Callable<Person>() {
				@Override
				public Person call() throws Exception {
					return new Person("Joe");
				}
			});
			this.futureTasks.add(futureTask);
			return futureTask;
		}

		public void onMessage(String name) {
			for (DeferredResult<Person> deferredResult : this.deferredResults) {
				deferredResult.setResult(new Person(name));
				this.deferredResults.remove(deferredResult);
			}
			for (ListenableFutureTask<Person> futureTask : this.futureTasks) {
				futureTask.run();
				this.futureTasks.remove(futureTask);
			}
		}
	}

}
