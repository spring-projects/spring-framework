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
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.PrintingResultHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Integration tests for {@link PrintingResultHandler}.
 *
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 * @since 5.3.10
 * @see PrintingResultHandlerSmokeTests
 * @see org.springframework.test.web.servlet.result.PrintingResultHandlerTests
 */
class PrintingResultHandlerIntegrationTests {

	@Test
	void printMvcResultsToWriter() throws Exception {
		StringWriter writer = new StringWriter();

		standaloneSetup(new SimpleController())
			.alwaysDo(print(writer))
			.build()
			.perform(get("/").content("Hello Request".getBytes()).characterEncoding("ISO-8859-1"))
			.andExpect(content().string("Hello Response"));

		assertThat(writer).asString()
			.contains("Hello Request")
			.contains("Hello Response")
			.contains("Headers = [Set-Cookie:\"enigma=42\", Content-Type:\"text/plain;charset=ISO-8859-1\", Content-Length:\"14\"]");
	}

	@Test
	void printMvcResultsToWriterWithJsonResponseBodyInterpretedAsUtf8() throws Exception {
		StringWriter writer = new StringWriter();

		standaloneSetup(new SimpleController()).build()
			// "Hallöchen" is German slang for "hello".
			.perform(get("/utf8").accept(MediaType.APPLICATION_JSON).content("Hallöchen, Welt!".getBytes(UTF_8)).characterEncoding(UTF_8))
			.andDo(print(writer))
			// "Grüß dich!" is German for "greetings to you".
			.andExpect(content().bytes("Grüß dich!".getBytes(UTF_8)));

		assertThat(writer).asString()
			.contains("Body = Hallöchen, Welt!")
			.contains("Body = Grüß dich!");
	}

	@Test
	void printMvcResultsToWriterWithFailingGlobalResultMatcher() throws Exception {
		StringWriter writer = new StringWriter();

		try {
			standaloneSetup(new SimpleController())
				.alwaysDo(print(writer))
				.alwaysExpect(content().string("Boom!"))
				.build()
				.perform(get("/").content("Hello Request".getBytes()).characterEncoding("ISO-8859-1"));
			fail("AssertionError is expected to be thrown.");
		}
		catch (AssertionError error) {
			assertThat(error).hasMessageContaining("Boom!");
		}

		assertThat(writer).asString()
			.contains("Hello Request")
			.contains("Hello Response")
			.contains("Headers = [Set-Cookie:\"enigma=42\", Content-Type:\"text/plain;charset=ISO-8859-1\", Content-Length:\"14\"]");
	}


	@RestController
	private static class SimpleController {

		@GetMapping("/")
		String hello(HttpServletResponse response) {
			response.addCookie(new Cookie("enigma", "42"));
			return "Hello Response";
		}

		@GetMapping("/utf8")
		String utf8(HttpServletResponse response) {
			return "Grüß dich!";
		}
	}

}
