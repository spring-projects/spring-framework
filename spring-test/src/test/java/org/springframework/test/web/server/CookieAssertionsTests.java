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

package org.springframework.test.web.server;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link CookieAssertions}
 *
 * @author Rob Worsnop
 */
public class CookieAssertionsTests {

	private final ResponseCookie cookie = ResponseCookie.from("foo", "bar")
			.maxAge(Duration.ofMinutes(30))
			.domain("foo.com")
			.path("/foo")
			.secure(true)
			.httpOnly(true)
			.partitioned(true)
			.sameSite("Lax")
			.build();

	private final CookieAssertions assertions = cookieAssertions(cookie);


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
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.value("foo", input -> assertThat(input).isEqualTo("what?!")));
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


	private CookieAssertions cookieAssertions(ResponseCookie cookie) {
		RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response = mock();
		var headers = new HttpHeaders();
		headers.set(HttpHeaders.SET_COOKIE, cookie.toString());
		when(response.getHeaders()).thenReturn(headers);
		ExchangeResult result = new ExchangeResult(response);
		return new CookieAssertions(result, mock());
	}

}
