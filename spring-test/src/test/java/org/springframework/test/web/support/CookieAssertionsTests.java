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

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseCookie;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link AbstractCookieAssertions}.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 */
public class CookieAssertionsTests {

	private TestCookieAssertions assertions;


	@BeforeEach
	void setUp() throws IOException {

		ResponseCookie cookie = ResponseCookie.from("foo", "bar")
				.maxAge(Duration.ofMinutes(30))
				.domain("foo.com")
				.path("/foo")
				.secure(true)
				.httpOnly(true)
				.partitioned(true)
				.sameSite("Lax")
				.build();

		this.assertions = initCookieAssertions(cookie);
	}


	@Test
	void valueEquals() {
		assertions.valueEquals("foo", "bar");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.valueEquals("what?!", "bar"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.valueEquals("foo", "what?!"));
	}

	@Test
	void value() {
		assertions.value("foo", equalTo("bar"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.value("foo", equalTo("what?!")));
	}

	@Test
	void valueConsumer() {
		assertions.value("foo", input -> assertThat(input).isEqualTo("bar"));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.value("foo", input -> assertThat(input).isEqualTo("what?!")));
	}

	@Test
	void exists() {
		assertions.exists("foo");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.exists("what?!"));
	}

	@Test
	void doesNotExist() {
		assertions.doesNotExist("what?!");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.doesNotExist("foo"));
	}

	@Test
	void maxAge() {
		assertions.maxAge("foo", Duration.ofMinutes(30));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.maxAge("foo", Duration.ofMinutes(29)));

		assertions.maxAge("foo", equalTo(Duration.ofMinutes(30).getSeconds()));
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.maxAge("foo", equalTo(Duration.ofMinutes(29).getSeconds())));
	}

	@Test
	void domain() {
		assertions.domain("foo", "foo.com");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.domain("foo", "what.com"));

		assertions.domain("foo", equalTo("foo.com"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.domain("foo", equalTo("what.com")));
	}

	@Test
	void path() {
		assertions.path("foo", "/foo");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.path("foo", "/what"));

		assertions.path("foo", equalTo("/foo"));
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.path("foo", equalTo("/what")));
	}

	@Test
	void secure() {
		assertions.secure("foo", true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.secure("foo", false));
	}

	@Test
	void httpOnly() {
		assertions.httpOnly("foo", true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.httpOnly("foo", false));
	}

	@Test
	void partitioned() {
		assertions.partitioned("foo", true);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.partitioned("foo", false));
	}

	@Test
	void sameSite() {
		assertions.sameSite("foo", "Lax");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.sameSite("foo", "Strict"));
	}

	private TestCookieAssertions initCookieAssertions(ResponseCookie cookie) throws IOException {
		return new TestCookieAssertions(cookie);
	}


	private static class TestCookieAssertions extends AbstractCookieAssertions<TestExchangeResult, Object> {

		TestCookieAssertions(ResponseCookie cookie) {
			super(new TestExchangeResult(cookie), "");
		}

		@Override
		protected MultiValueMap<String, ResponseCookie> getResponseCookies() {
			ResponseCookie cookie = getExchangeResult().cookie();
			return MultiValueMap.fromSingleValue(Map.of(cookie.getName(), cookie));
		}

		@Override
		protected void assertWithDiagnostics(Runnable assertion) {
			assertion.run();
		}
	}


	private record TestExchangeResult(ResponseCookie cookie) {
	}

}
