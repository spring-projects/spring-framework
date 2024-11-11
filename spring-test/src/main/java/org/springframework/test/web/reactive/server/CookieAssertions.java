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

package org.springframework.test.web.reactive.server;

import java.time.Duration;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.ResponseCookie;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on cookies of the response.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class CookieAssertions {

	private final ExchangeResult exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	public CookieAssertions(ExchangeResult exchangeResult, WebTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * Expect a response cookie with the given name to match the specified value.
	 */
	public WebTestClient.ResponseSpec valueEquals(String name, String value) {
		String cookieValue = getCookie(name).getValue();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertEquals(message, value, cookieValue);
		});
		return this.responseSpec;
	}

	/**
	 * Assert the value of the response cookie with the given name with a Hamcrest
	 * {@link Matcher}.
	 */
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getCookie(name).getValue();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name);
			MatcherAssert.assertThat(message, value, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Consume the value of the response cookie with the given name.
	 */
	public WebTestClient.ResponseSpec value(String name, Consumer<String> consumer) {
		String value = getCookie(name).getValue();
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	/**
	 * Expect that the cookie with the given name is present.
	 */
	public WebTestClient.ResponseSpec exists(String name) {
		getCookie(name);
		return this.responseSpec;
	}

	/**
	 * Expect that the cookie with the given name is not present.
	 */
	public WebTestClient.ResponseSpec doesNotExist(String name) {
		ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
		if (cookie != null) {
			String message = getMessage(name) + " exists with value=[" + cookie.getValue() + "]";
			this.exchangeResult.assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Max-Age" attribute.
	 */
	public WebTestClient.ResponseSpec maxAge(String name, Duration expected) {
		Duration maxAge = getCookie(name).getMaxAge();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertEquals(message, expected, maxAge);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Max-Age" attribute with a Hamcrest {@link Matcher}.
	 */
	public WebTestClient.ResponseSpec maxAge(String name, Matcher<? super Long> matcher) {
		long maxAge = getCookie(name).getMaxAge().getSeconds();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertThat(message, maxAge, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Path" attribute.
	 */
	public WebTestClient.ResponseSpec path(String name, String expected) {
		String path = getCookie(name).getPath();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Path" attribute with a Hamcrest {@link Matcher}.
	 */
	public WebTestClient.ResponseSpec path(String name, Matcher<? super String> matcher) {
		String path = getCookie(name).getPath();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertThat(message, path, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Domain" attribute.
	 */
	public WebTestClient.ResponseSpec domain(String name, String expected) {
		String path = getCookie(name).getDomain();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Domain" attribute with a Hamcrest {@link Matcher}.
	 */
	public WebTestClient.ResponseSpec domain(String name, Matcher<? super String> matcher) {
		String domain = getCookie(name).getDomain();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertThat(message, domain, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Secure" attribute.
	 */
	public WebTestClient.ResponseSpec secure(String name, boolean expected) {
		boolean isSecure = getCookie(name).isSecure();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " secure";
			assertEquals(message, expected, isSecure);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "HttpOnly" attribute.
	 */
	public WebTestClient.ResponseSpec httpOnly(String name, boolean expected) {
		boolean isHttpOnly = getCookie(name).isHttpOnly();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " httpOnly";
			assertEquals(message, expected, isHttpOnly);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Partitioned" attribute.
	 * @since 6.2
	 */
	public WebTestClient.ResponseSpec partitioned(String name, boolean expected) {
		boolean isPartitioned = getCookie(name).isPartitioned();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " isPartitioned";
			assertEquals(message, expected, isPartitioned);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "SameSite" attribute.
	 */
	public WebTestClient.ResponseSpec sameSite(String name, String expected) {
		String sameSite = getCookie(name).getSameSite();
		this.exchangeResult.assertWithDiagnostics(() -> {
			String message = getMessage(name) + " sameSite";
			assertEquals(message, expected, sameSite);
		});
		return this.responseSpec;
	}


	private ResponseCookie getCookie(String name) {
		ResponseCookie cookie = this.exchangeResult.getResponseCookies().getFirst(name);
		if (cookie != null) {
			return cookie;
		}
		else {
			this.exchangeResult.assertWithDiagnostics(() -> fail("No cookie with name '" + name + "'"));
		}
		throw new IllegalStateException("This code path should not be reachable");
	}

	private static String getMessage(String cookie) {
		return "Response cookie '" + cookie + "'";
	}

}
