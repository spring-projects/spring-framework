/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Examples of expectations on the status and the status reason found in the response.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class StatusAssertionTests {

	private final MockMvc mockMvc = standaloneSetup(new StatusController()).build();

	@Test
	public void testStatusInt() throws Exception {
		this.mockMvc.perform(get("/created")).andExpect(status().is(201));
		this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().is(201));
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(400));
	}

	@Test
	public void testHttpStatus() throws Exception {
		this.mockMvc.perform(get("/created")).andExpect(status().isCreated());
		this.mockMvc.perform(get("/createdWithComposedAnnotation")).andExpect(status().isCreated());
		this.mockMvc.perform(get("/badRequest")).andExpect(status().isBadRequest());
	}

	@Test
	public void testMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().is(equalTo(400)));
	}

	@Test
	public void testReasonEqualTo() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason("Expired token"));

		// Hamcrest matchers...
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(equalTo("Expired token")));
	}

	@Test
	public void testReasonMatcher() throws Exception {
		this.mockMvc.perform(get("/badRequest")).andExpect(status().reason(endsWith("token")));
	}


	@RequestMapping
	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@interface Get {

		@AliasFor(annotation = RequestMapping.class, attribute = "path")
		String[] path() default {};

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus status() default INTERNAL_SERVER_ERROR;
	}

	@Controller
	private static class StatusController {

		@RequestMapping("/created")
		@ResponseStatus(CREATED)
		public @ResponseBody void created(){
		}

		@Get(path = "/createdWithComposedAnnotation", status = CREATED)
		public @ResponseBody void createdWithComposedAnnotation() {
		}

		@RequestMapping("/badRequest")
		@ResponseStatus(code = BAD_REQUEST, reason = "Expired token")
		public @ResponseBody void badRequest(){
		}

		@RequestMapping("/notImplemented")
		@ResponseStatus(NOT_IMPLEMENTED)
		public @ResponseBody void notImplemented(){
		}
	}

}
