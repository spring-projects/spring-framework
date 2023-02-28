/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.test.web.servlet.samples.standalone.resulthandlers;

import java.io.StringWriter;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Smoke test for {@link PrintingResultHandler}.
 *
 * <p>Prints debugging information about the executed request and response to
 * various output streams.
 *
 * <p><strong>NOTE</strong>: this <em>smoke test</em> is not intended to be
 * executed with the build. To run this test, comment out the {@code @Disabled}
 * declaration and inspect the output manually.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see org.springframework.test.web.servlet.result.PrintingResultHandlerTests
 */
@Disabled
public class PrintingResultHandlerSmokeTests {

	// Not intended to be executed with the build.
	// Comment out class-level @Disabled to see the output.

	@Test
	public void testPrint() throws Exception {
		StringWriter writer = new StringWriter();

		standaloneSetup(new SimpleController())
			.build()
			.perform(get("/").content("Hello Request".getBytes()))
			.andDo(log())
			.andDo(print())
			.andDo(print(System.err))
			.andDo(print(writer))
		;

		System.out.println();
		System.out.println("===============================================================");
		System.out.println(writer);
	}


	@Controller
	private static class SimpleController {

		@RequestMapping("/")
		@ResponseBody
		public String hello(HttpServletResponse response) {
			response.addCookie(new Cookie("enigma", "42"));
			return "Hello Response";
		}
	}
}
