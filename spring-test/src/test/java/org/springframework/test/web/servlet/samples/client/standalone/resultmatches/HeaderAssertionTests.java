/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web.servlet.samples.client.standalone.resultmatches;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.Person;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.HttpHeaders.IF_MODIFIED_SINCE;
import static org.springframework.http.HttpHeaders.LAST_MODIFIED;
import static org.springframework.http.HttpHeaders.VARY;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link org.springframework.test.web.servlet.samples.standalone.resultmatchers.HeaderAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderAssertionTests {

	private static final String ERROR_MESSAGE = "Should have thrown an AssertionError";


	private String now;

	private String minuteAgo;

	private WebTestClient testClient;

	private final long currentTime = System.currentTimeMillis();

	private SimpleDateFormat dateFormat;


	@BeforeEach
	public void setup() {
		this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.now = dateFormat.format(new Date(this.currentTime));
		this.minuteAgo = dateFormat.format(new Date(this.currentTime - (1000 * 60)));

		PersonController controller = new PersonController();
		controller.setStubTimestamp(this.currentTime);
		this.testClient = MockMvcWebTestClient.bindToController(controller).build();
	}


	@Test
	public void stringWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(LAST_MODIFIED, now);
	}

	@Test
	public void stringWithMatcherAndCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value(LAST_MODIFIED, equalTo(now));
	}

	@Test
	public void multiStringHeaderValue() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(VARY, "foo", "bar");
	}

	@Test
	public void multiStringHeaderValueWithMatchers() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().values(VARY, hasItems(containsString("foo"), startsWith("bar")));
	}

	@Test
	public void dateValueWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1")
				.header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEqualsDate(LAST_MODIFIED, this.currentTime);
	}

	@Test
	public void longValueWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("X-Rate-Limiting", 42);
	}

	@Test
	public void stringWithMissingResponseHeader() {
		testClient.get().uri("/persons/1")
				.header(IF_MODIFIED_SINCE, now)
				.exchange()
				.expectStatus().isNotModified()
				.expectHeader().valueEquals("X-Custom-Header");
	}

	@Test
	public void stringWithMatcherAndMissingResponseHeader() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, now)
				.exchange()
				.expectStatus().isNotModified()
				.expectHeader().value("X-Custom-Header", nullValue());
	}

	@Test
	public void longValueWithMissingResponseHeader() {
		try {
			testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, now)
					.exchange()
					.expectStatus().isNotModified()
					.expectHeader().valueEquals("X-Custom-Header", 99L);

			fail(ERROR_MESSAGE);
		}
		catch (AssertionError err) {
			if (ERROR_MESSAGE.equals(err.getMessage())) {
				throw err;
			}
			assertThat(err.getMessage()).startsWith("Response does not contain header 'X-Custom-Header'");
		}
	}

	@Test
	public void exists() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().exists(LAST_MODIFIED);
	}

	@Test
	public void existsFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().exists("X-Custom-Header"));
	}

	@Test
	public void doesNotExist() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().doesNotExist("X-Custom-Header");
	}

	@Test
	public void doesNotExistFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().doesNotExist(LAST_MODIFIED));
	}

	@Test
	public void longValueWithIncorrectResponseHeaderValue() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().valueEquals("X-Rate-Limiting", 1));
	}

	@Test
	public void stringWithMatcherAndIncorrectResponseHeaderValue() {
		long secondLater = this.currentTime + 1000;
		String expected = this.dateFormat.format(new Date(secondLater));
		assertIncorrectResponseHeader(spec -> spec.expectHeader().valueEquals(LAST_MODIFIED, expected), expected);
		assertIncorrectResponseHeader(spec -> spec.expectHeader().value(LAST_MODIFIED, equalTo(expected)), expected);
		// Comparison by date uses HttpHeaders to format the date in the error message.
		HttpHeaders headers = new HttpHeaders();
		headers.setDate("expected", secondLater);
		assertIncorrectResponseHeader(spec -> spec.expectHeader().valueEqualsDate(LAST_MODIFIED, secondLater), expected);
	}

	private void assertIncorrectResponseHeader(Consumer<WebTestClient.ResponseSpec> assertions, String expected) {
		try {
			WebTestClient.ResponseSpec spec = testClient.get().uri("/persons/1")
					.header(IF_MODIFIED_SINCE, minuteAgo)
					.exchange()
					.expectStatus().isOk();

			assertions.accept(spec);

			fail(ERROR_MESSAGE);
		}
		catch (AssertionError err) {
			if (ERROR_MESSAGE.equals(err.getMessage())) {
				throw err;
			}
			assertMessageContains(err, "Response header '" + LAST_MODIFIED + "'");
			assertMessageContains(err, expected);
			assertMessageContains(err, this.now);
		}
	}

	private void assertMessageContains(AssertionError error, String expected) {
		assertThat(error.getMessage())
				.as("Failure message should contain [" + expected + "], actual is [" + error.getMessage() + "]")
				.contains(expected);
	}


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
					.lastModified(this.timestamp)
					.header("X-Rate-Limiting", "42")
					.header("Vary", "foo", "bar")
					.body(new Person("Jason"));
		}
	}

}
