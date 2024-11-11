/*
 * Copyright 2002-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
class HeaderAssertionTests {

	private String now;

	private String minuteAgo;

	private WebTestClient testClient;

	private final long currentTime = System.currentTimeMillis();

	private SimpleDateFormat dateFormat;


	@BeforeEach
	void setup() {
		this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		this.now = dateFormat.format(new Date(this.currentTime));
		this.minuteAgo = dateFormat.format(new Date(this.currentTime - (1000 * 60)));

		PersonController controller = new PersonController();
		controller.setStubTimestamp(this.currentTime);
		this.testClient = MockMvcWebTestClient.bindToController(controller).build();
	}


	@Test
	void stringWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(LAST_MODIFIED, now);
	}

	@Test
	void stringWithMatcherAndCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().value(LAST_MODIFIED, equalTo(now));
	}

	@Test
	void multiStringHeaderValue() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals(VARY, "foo", "bar");
	}

	@Test
	void multiStringHeaderValueWithMatchers() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().values(VARY, hasItems(containsString("foo"), startsWith("bar")));
	}

	@Test
	void dateValueWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1")
				.header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEqualsDate(LAST_MODIFIED, this.currentTime);
	}

	@Test
	void longValueWithCorrectResponseHeaderValue() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().valueEquals("X-Rate-Limiting", 42);
	}

	@Test
	void stringWithMissingResponseHeader() {
		testClient.get().uri("/persons/1")
				.header(IF_MODIFIED_SINCE, now)
				.exchange()
				.expectStatus().isNotModified()
				.expectHeader().valueEquals("X-Custom-Header");
	}

	@Test
	void stringWithMatcherAndMissingResponseHeader() {
		testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, now)
				.exchange()
				.expectStatus().isNotModified()
				.expectHeader().value("X-Custom-Header", nullValue());
	}

	@Test
	void longValueWithMissingResponseHeader() {
		String headerName = "X-Custom-Header";
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
					testClient.get().uri("/persons/1").header(IF_MODIFIED_SINCE, now)
							.exchange()
							.expectStatus().isNotModified()
							.expectHeader().valueEquals(headerName, 99L))
				.withMessage("Response does not contain header '%s'", headerName);
	}

	@Test
	void exists() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().exists(LAST_MODIFIED);
	}

	@Test
	void existsFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().exists("X-Custom-Header"));
	}

	@Test
	void doesNotExist() {
		testClient.get().uri("/persons/1")
				.exchange()
				.expectStatus().isOk()
				.expectHeader().doesNotExist("X-Custom-Header");
	}

	@Test
	void doesNotExistFail() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().doesNotExist(LAST_MODIFIED));
	}

	@Test
	void longValueWithIncorrectResponseHeaderValue() {
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				testClient.get().uri("/persons/1")
						.exchange()
						.expectStatus().isOk()
						.expectHeader().valueEquals("X-Rate-Limiting", 1));
	}

	@Test
	void stringWithMatcherAndIncorrectResponseHeaderValue() {
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
		WebTestClient.ResponseSpec spec = testClient.get().uri("/persons/1")
				.header(IF_MODIFIED_SINCE, minuteAgo)
				.exchange()
				.expectStatus().isOk();
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.accept(spec))
				.withMessageContainingAll("Response header '" + LAST_MODIFIED + "'", expected, this.now);
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
