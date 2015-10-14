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

package org.springframework.test.web.servlet.samples.standalone.resultmatchers;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Examples of expectations on response header values.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Brian Clozel
 */
public class HeaderAssertionTests {

	private static final String EXPECTED_ASSERTION_ERROR_MSG = "Should have thrown an AssertionError";

	private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

	private static final String LAST_MODIFIED = "Last-Modified";

	private final long currentTime = System.currentTimeMillis();

	private String now;

	private String oneMinuteAgo;

	private String oneSecondLater;

	private MockMvc mockMvc;

	private PersonController personController;


	@Before
	public void setup() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.now = dateFormat.format(new Date(currentTime));
		this.oneMinuteAgo = dateFormat.format(new Date(currentTime - (1000 * 60)));
		this.oneSecondLater = dateFormat.format(new Date(currentTime + 1000));
		this.personController = new PersonController();
		this.personController.setStubTimestamp(currentTime);
		this.mockMvc = standaloneSetup(this.personController).build();
	}

	@Test
	public void stringWithCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, oneMinuteAgo))//
		.andExpect(header().string(LAST_MODIFIED, now));
	}

	@Test
	public void stringWithMatcherAndCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, oneMinuteAgo))//
		.andExpect(header().string(LAST_MODIFIED, equalTo(now)));
	}

	@Test
	public void dateValueWithCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, oneMinuteAgo))//
		.andExpect(header().dateValue(LAST_MODIFIED, currentTime));
	}

	@Test
	public void longValueWithCorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1"))//
		.andExpect(header().longValue("X-Rate-Limiting", 42));
	}

	@Test
	public void stringWithMissingResponseHeader() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))//
		.andExpect(status().isNotModified())//
		.andExpect(header().string("X-Custom-Header", (String) null));
	}

	@Test
	public void stringWithMatcherAndMissingResponseHeader() throws Exception {
		this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))//
		.andExpect(status().isNotModified())//
		.andExpect(header().string("X-Custom-Header", nullValue()));
	}

	@Test
	public void longValueWithMissingResponseHeader() throws Exception {
		try {
			this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, now))//
			.andExpect(status().isNotModified())//
			.andExpect(header().longValue("X-Custom-Header", 99L));

			fail(EXPECTED_ASSERTION_ERROR_MSG);
		}
		catch (AssertionError e) {
			if (EXPECTED_ASSERTION_ERROR_MSG.equals(e.getMessage())) {
				throw e;
			}
			assertEquals("Response does not contain header " + "X-Custom-Header", e.getMessage());
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
		assertIncorrectResponseHeaderValue(header().string(LAST_MODIFIED, oneSecondLater), oneSecondLater);
	}

	@Test
	public void stringWithMatcherAndIncorrectResponseHeaderValue() throws Exception {
		assertIncorrectResponseHeaderValue(header().string(LAST_MODIFIED, equalTo(oneSecondLater)), oneSecondLater);
	}

	@Test
	public void dateValueWithIncorrectResponseHeaderValue() throws Exception {
		long unexpected = currentTime + 1000;
		assertIncorrectResponseHeaderValue(header().dateValue(LAST_MODIFIED, unexpected), oneSecondLater);
	}

	@Test(expected = AssertionError.class)
	public void longValueWithIncorrectResponseHeaderValue() throws Exception {
		this.mockMvc.perform(get("/persons/1")).andExpect(header().longValue("X-Rate-Limiting", 1));
	}

	private void assertIncorrectResponseHeaderValue(ResultMatcher resultMatcher, String unexpected) throws Exception {
		try {
			this.mockMvc.perform(get("/persons/1").header(IF_MODIFIED_SINCE, oneMinuteAgo))//
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
			assertMessageContains(e, unexpected);
			assertMessageContains(e, now);
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
		public ResponseEntity<Person> showEntity(@PathVariable long id, WebRequest request) {
			return ResponseEntity
					.ok()
					.lastModified(calculateLastModified(id))
					.header("X-Rate-Limiting", "42")
					.body(new Person("Jason"));
		}

		private long calculateLastModified(long id) {
			return this.timestamp;
		}
	}
}
