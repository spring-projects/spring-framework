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

package org.springframework.test.web.servlet.samples.standalone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Exception handling via {@code @ExceptionHandler} method.
 *
 * @author Rossen Stoyanchev
 */
public class ExceptionHandlerTests {

	@Test
	public void testExceptionHandlerMethod() throws Exception {
		standaloneSetup(new PersonController()).build()
			.perform(get("/person/Clyde"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("errorView"));
	}


	@Controller
	private static class PersonController {

		@RequestMapping(value="/person/{name}", method=RequestMethod.GET)
		public String show(@PathVariable String name) {
			if (name.equals("Clyde")) {
				throw new IllegalArgumentException("Black listed");
			}
			return "person/show";
		}

		@ExceptionHandler
		public String handleException(IllegalArgumentException exception) {
			return "errorView";
		}
	}
}
