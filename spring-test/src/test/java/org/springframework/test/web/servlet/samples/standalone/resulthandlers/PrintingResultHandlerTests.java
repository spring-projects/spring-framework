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

package org.springframework.test.web.servlet.samples.standalone.resulthandlers;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Print debugging information about the executed request and response to System.out.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
@Ignore("Not intended to be executed with the build. Comment out this line to inspect the output manually.")
public class PrintingResultHandlerTests {

	@Test
	public void testPrint() throws Exception {
		standaloneSetup(new SimpleController()).build().perform(get("/")).andDo(print());
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		@ResponseBody
		public String hello() {
			return "Hello world";
		}
	}
}
