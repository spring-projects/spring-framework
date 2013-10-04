/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

/**
 * Examples of expectations on response header values.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class HeaderAssertionTests {

	private static final String EXPECTED_ASSERTION_ERROR_MSG = "Should have thrown an AssertionError";

	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String LAST_MODIFIED = "Last-Modified";

	private final long currentTime = System.currentTimeMillis();

	private MockMvc mockMvc;

	private PersonController personController;


	@Before
	public void setup() {
		this.personController = new PersonController();
		this.personController.setStubTimestamp(currentTime);
		this.mockMvc = standaloneSetup(this.personController).build();
	}

	@Test
	public void stringWithCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime - (1000 * 60)))//
		.andExpect(header().string(LAST_MODIFIED, String.valueOf(currentTime)));
	}

	@Test
	public void stringWithMatcherAndCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime - (1000 * 60)))//
		.andExpect(header().string(LAST_MODIFIED, equalTo(String.valueOf(currentTime))));
	}

	@Test
	public void longValueWithCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime - (1000 * 60)))//
		.andExpect(header().longValue(LAST_MODIFIED, currentTime));
	}

	@Test
	public void stringWithMissingResponseHeader() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime))//
		.andExpect(status().isNotModified())//
		.andExpect(header().string(LAST_MODIFIED, (String) null));
	}

	@Test
	public void stringWithMatcherAndMissingResponseHeader() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime))//
		.andExpect(status().isNotModified())//
		.andExpect(header().string(LAST_MODIFIED, nullValue()));
	}

	@Test
	public void longValueWithMissingResponseHeader() throws Exception {
		try {
			this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime))//
			.andExpect(status().isNotModified())//
			.andExpect(header().longValue(LAST_MODIFIED, 99L));

			fail(EXPECTED_ASSERTION_ERROR_MSG);
		}
		catch (AssertionError e) {
			if (EXPECTED_ASSERTION_ERROR_MSG.equals(e.getMessage())) {
				throw e;
			}
			assertEquals("Response does not contain header " + LAST_MODIFIED, e.getMessage());
		}
	}

	// SPR-10771

	@Test
	public void doesNotExist() throws Exception {
		this.mockMvc.perform(get("/persons/1"))
				.andExpect(header().doesNotExist("X-Custom-Header"));
	}

	// SPR-10771

	@Test(expected = AssertionError.class)
	public void doesNotExistFail() throws Exception {
		this.mockMvc.perform(get("/persons/1"))
				.andExpect(header().doesNotExist(LAST_MODIFIED));
	}

	@Test
	public void stringWithIncorrectResponseHeaderValue() throws Exception {
		long unexpected = currentTime + 1;
		assertIncorrectResponseHeaderValue(header().string(LAST_MODIFIED, String.valueOf(unexpected)), unexpected);
	}

	@Test
	public void stringWithMatcherAndIncorrectResponseHeaderValue() throws Exception {
		long unexpected = currentTime + 1;
		assertIncorrectResponseHeaderValue(header().string(LAST_MODIFIED, equalTo(String.valueOf(unexpected))),
			unexpected);
	}

	@Test
	public void longValueWithIncorrectResponseHeaderValue() throws Exception {
		long unexpected = currentTime + 1;
		assertIncorrectResponseHeaderValue(header().longValue(LAST_MODIFIED, unexpected), unexpected);
	}

	private void assertIncorrectResponseHeaderValue(ResultMatcher resultMatcher, long unexpected) throws Exception {
		try {
			this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, currentTime - (1000 * 60)))//
			.andExpect(resultMatcher);

			fail(EXPECTED_ASSERTION_ERROR_MSG);
		}
		catch (AssertionError e) {
			if (EXPECTED_ASSERTION_ERROR_MSG.equals(e.getMessage())) {
				throw e;
			}
			// [SPR-10659] Ensure that the header name is included in the message
			//
			// We don't use assertEquals() since we cannot control the formatting
			// produced by JUnit or Hamcrest.
			assertMessageContains(e, "Response header " + LAST_MODIFIED);
			assertMessageContains(e, String.valueOf(unexpected));
			assertMessageContains(e, String.valueOf(currentTime));
		}
	}

	private void assertMessageContains(AssertionError error, String expected) {
		String message = error.getMessage();
		assertTrue("Failure message should contain: " + expected, message.contains(expected));
	}


	// -------------------------------------------------------------------------

	@Controller
	private static class PersonController {

		private long timestamp;


		public void setStubTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		@RequestMapping("/persons/{id}")
		@ResponseBody
		public Person showEntity(@PathVariable long id, WebRequest request) {
			if (request.checkNotModified(calculateLastModified(id))) {
				return null;
			}
			return new Person("Jason");
		}

		private long calculateLastModified(long id) {
			return this.timestamp;
		}
	}
}
