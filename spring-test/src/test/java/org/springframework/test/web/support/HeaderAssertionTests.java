/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.support;

import java.net.URI;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

/**
 * Tests for {@link AbstractHeaderAssertions}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 */
class HeaderAssertionTests {

	@Test
	void valueEquals() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		headers.add("age", "22");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar");
		assertions.value("foo", s -> assertThat(s).isEqualTo("bar"));
		assertions.values("foo", strings -> assertThat(strings).containsExactly("bar"));
		assertions.valueEquals("age", 22);

		// Missing header
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEquals("what?!", "bar"));

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEquals("foo", "what?!"));

		// Wrong # of values
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEquals("foo", "bar", "what?!"));
	}

	@Test
	void valueEqualsWithMultipleValues() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		headers.add("foo", "baz");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.valueEquals("foo", "bar", "baz");

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEquals("foo", "bar", "what?!"));

		// Too few values
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEquals("foo", "bar"));
	}

	@Test
	void valueMatches() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.valueMatches("Content-Type", ".*UTF-8.*");

		// Wrong pattern
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueMatches("Content-Type", ".*ISO-8859-1.*"))
				.satisfies(ex -> assertThat(ex).hasMessage("Response header " +
						"'Content-Type'=[application/json;charset=UTF-8] does not match " +
						"[.*ISO-8859-1.*]"));
	}

	@Test
	void valueMatchesWithNonexistentHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueMatches("Content-XYZ", ".*ISO-8859-1.*"))
				.withMessage("Response header 'Content-XYZ' not found");
	}

	@Test
	void valuesMatch() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "value1");
		headers.add("foo", "value2");
		headers.add("foo", "value3");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		assertions.valuesMatch("foo", "val.*1", "val.*2", "val.*3");

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valuesMatch("foo", ".*", "val.*5"))
				.satisfies(ex -> assertThat(ex).hasMessage(
						"Response header 'foo' has fewer or more values [value1, value2, value3] " +
								"than number of patterns to match with [.*, val.*5]"));

		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valuesMatch("foo", ".*", "val.*5", ".*"))
				.satisfies(ex -> assertThat(ex).hasMessage(
						"Response header 'foo'[1]='value2' does not match 'val.*5'"));
	}

	@Test
	void valueMatcher() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		assertions.value("foo", containsString("a"));
	}

	@Test
	void valuesMatcher() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("foo", "bar");
		headers.add("foo", "baz");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		assertions.values("foo", hasItems("bar", "baz"));
	}

	@Test
	void exists() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.exists("Content-Type");

		// Header should not exist
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.exists("Framework"))
				.satisfies(ex -> assertThat(ex).hasMessage("Response header 'Framework' does not exist"));
	}

	@Test
	void doesNotExist() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.parseMediaType("application/json;charset=UTF-8"));
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.doesNotExist("Framework");

		// Existing header
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.doesNotExist("Content-Type"))
				.satisfies(ex -> assertThat(ex).hasMessage("Response header " +
						"'Content-Type' exists with value=[application/json;charset=UTF-8]"));
	}

	@Test
	void contentTypeCompatibleWith() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.contentTypeCompatibleWith(MediaType.parseMediaType("application/*"));
		assertions.contentTypeCompatibleWith("application/*");

		// MediaTypes not compatible
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.contentTypeCompatibleWith(MediaType.TEXT_XML))
				.withMessage("Response header 'Content-Type'=[application/xml] is not compatible with [text/xml]");
	}

	@Test
	void location() {
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(URI.create("http://localhost:8080/"));
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.location("http://localhost:8080/");

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.location("http://localhost:8081/"));
	}

	@Test
	void cacheControl() {
		CacheControl control = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform();

		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(control.getHeaderValue());
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);

		// Success
		assertions.cacheControl(control);

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.cacheControl(CacheControl.noStore()));
	}

	@Test
	void contentDisposition() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentDispositionFormData("foo", "bar");
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.contentDisposition(ContentDisposition.formData().name("foo").filename("bar").build());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.contentDisposition(ContentDisposition.attachment().build()));
	}

	@Test
	void contentLength() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentLength(100);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.contentLength(100);

		// Wrong value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.contentLength(200));
	}

	@Test
	void contentType() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.contentType(MediaType.APPLICATION_JSON);
		assertions.contentType("application/json");

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.contentType(MediaType.APPLICATION_XML));
	}


	@Test
	void expires() {
		HttpHeaders headers = new HttpHeaders();
		ZonedDateTime expires = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setExpires(expires);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.expires(expires.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.expires(expires.toInstant().toEpochMilli() + 1));
	}

	@Test
	void lastModified() {
		HttpHeaders headers = new HttpHeaders();
		ZonedDateTime lastModified = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
		headers.setLastModified(lastModified.toInstant().toEpochMilli());
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.lastModified(lastModified.toInstant().toEpochMilli());

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.lastModified(lastModified.toInstant().toEpochMilli() + 1));
	}

	@Test
	void equalsDate() {
		HttpHeaders headers = new HttpHeaders();
		headers.setDate("foo", 1000);
		TestHeaderAssertions assertions = new TestHeaderAssertions(headers);
		assertions.valueEqualsDate("foo", 1000);

		// Wrong value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.valueEqualsDate("foo", 2000));
	}


	private static class TestHeaderAssertions extends AbstractHeaderAssertions<TestExchangeResult, Object> {

		TestHeaderAssertions(HttpHeaders headers) {
			super(new TestExchangeResult(headers), "");
		}

		@Override
		protected HttpHeaders getResponseHeaders() {
			return getExchangeResult().headers();
		}

		@Override
		protected void assertWithDiagnostics(Runnable assertion) {
			assertion.run();
		}
	}


	private record TestExchangeResult(HttpHeaders headers) {}

}
