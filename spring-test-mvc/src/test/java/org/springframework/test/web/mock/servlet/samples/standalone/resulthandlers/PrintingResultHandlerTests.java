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

package org.springframework.test.web.mock.servlet.samples.standalone.resulthandlers;

import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Print debugging information about the executed request and response to System.out.
 *
 * @author Rossen Stoyanchev
 */
public class PrintingResultHandlerTests {

	@Test
	public void testPrint() throws Exception {

		// Not testing anything, uncomment to see the output

		// standaloneSetup(new SimpleController()).build().perform(get("/")).andDo(print());
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
