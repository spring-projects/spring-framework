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

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Examples of expectations on the handler or handler method that executed the request.
 *
 * <p>Note that in most cases "handler" is synonymous with "controller".
 * For example an {@code @Controller} is a kind of handler.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerAssertionTests {

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = standaloneSetup(new SimpleController()).alwaysExpect(status().isOk()).build();
	}

	@Test
	public void testHandlerType() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().handlerType(SimpleController.class));
	}

	@Test
	public void testHandlerMethodNameEqualTo() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().methodName("handle"));

		// Hamcrest matcher..
		this.mockMvc.perform(get("/")).andExpect(handler().methodName(equalTo("handle")));
	}

	@Test
	public void testHandlerMethodNameMatcher() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(handler().methodName(is(not("save"))));
	}

	@Test
	public void testHandlerMethod() throws Exception {
		Method method = SimpleController.class.getMethod("handle");
		this.mockMvc.perform(get("/")).andExpect(handler().method(method));
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		public String handle() {
			return "view";
		}
	}
}
