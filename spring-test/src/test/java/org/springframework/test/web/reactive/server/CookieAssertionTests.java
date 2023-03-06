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

package org.springframework.test.web.reactive.server;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link CookieAssertions}
 * @author Rossen Stoyanchev
 */
public class CookieAssertionTests {

	private final ResponseCookie cookie = ResponseCookie.from("foo", "bar")
			.maxAge(Duration.ofMinutes(30))
			.domain("foo.com")
			.path("/foo")
			.secure(true)
			.httpOnly(true)
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
	void sameSite() {
		assertions.sameSite("foo", "Lax");
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.sameSite("foo", "Strict"));
	}


	private CookieAssertions cookieAssertions(ResponseCookie cookie) {
		MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("/"));
		MockClientHttpResponse response = new MockClientHttpResponse(HttpStatus.OK);
		response.getCookies().add(cookie.getName(), cookie);

		ExchangeResult result = new ExchangeResult(
				request, response, Mono.empty(), Mono.empty(), Duration.ZERO, null, null);

		return new CookieAssertions(result, mock());
	}

}
