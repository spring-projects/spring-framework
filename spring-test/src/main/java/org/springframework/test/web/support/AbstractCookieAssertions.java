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

import java.time.Duration;
import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.ResponseCookie;
import org.springframework.test.util.AssertionErrors;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.util.AssertionErrors.fail;

/**
 * Assertions on cookies of the response.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <E> the type of the exchange result
 * @param <R> the type of the response spec
 */
public abstract class AbstractCookieAssertions<E, R> {

	private final E exchangeResult;

	private final R responseSpec;


	protected AbstractCookieAssertions(E exchangeResult, R responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * Return the exchange result.
	 */
	protected E getExchangeResult() {
		return this.exchangeResult;
	}

	/**
	 * Subclasses must implement this to provide access to response cookies.
	 */
	protected abstract MultiValueMap<String, ResponseCookie> getResponseCookies();

	/**
	 * Subclasses must implement this to assert with diagnostics.
	 */
	protected abstract void assertWithDiagnostics(Runnable assertion);


	/**
	 * Expect a response cookie with the given name to match the specified value.
	 */
	public R valueEquals(String name, String value) {
		ResponseCookie cookie = getCookie(name);
		String cookieValue = cookie.getValue();
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			AssertionErrors.assertEquals(message, value, cookieValue);
		});
		return this.responseSpec;
	}

	/**
	 * Assert the value of the response cookie with the given name with a Hamcrest
	 * {@link Matcher}.
	 */
	public R value(String name, Matcher<? super String> matcher) {
		String value = getCookie(name).getValue();
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			MatcherAssert.assertThat(message, value, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Consume the value of the response cookie with the given name.
	 */
	public R value(String name, Consumer<String> consumer) {
		String value = getCookie(name).getValue();
		assertWithDiagnostics(() -> consumer.accept(value));
		return this.responseSpec;
	}

	/**
	 * Expect that the cookie with the given name is present.
	 */
	public R exists(String name) {
		getCookie(name);
		return this.responseSpec;
	}

	/**
	 * Expect that the cookie with the given name is not present.
	 */
	public R doesNotExist(String name) {
		ResponseCookie cookie = getResponseCookies().getFirst(name);
		if (cookie != null) {
			String message = getMessage(name) + " exists with value=[" + cookie.getValue() + "]";
			assertWithDiagnostics(() -> fail(message));
		}
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Max-Age" attribute.
	 */
	public R maxAge(String name, Duration expected) {
		Duration maxAge = getCookie(name).getMaxAge();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertEquals(message, expected, maxAge);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Max-Age" attribute with a Hamcrest {@link Matcher}.
	 */
	public R maxAge(String name, Matcher<? super Long> matcher) {
		long maxAge = getCookie(name).getMaxAge().getSeconds();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertThat(message, maxAge, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Path" attribute.
	 */
	public R path(String name, String expected) {
		String path = getCookie(name).getPath();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}



	/**
	 * Assert a cookie's "Path" attribute with a Hamcrest {@link Matcher}.
	 */
	public R path(String name, Matcher<? super String> matcher) {
		String path = getCookie(name).getPath();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertThat(message, path, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Domain" attribute.
	 */
	public R domain(String name, String expected) {
		String path = getCookie(name).getDomain();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertEquals(message, expected, path);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Domain" attribute with a Hamcrest {@link Matcher}.
	 */
	public R domain(String name, Matcher<? super String> matcher) {
		String domain = getCookie(name).getDomain();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertThat(message, domain, matcher);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Secure" attribute.
	 */
	public R secure(String name, boolean expected) {
		boolean isSecure = getCookie(name).isSecure();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " secure";
			assertEquals(message, expected, isSecure);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "HttpOnly" attribute.
	 */
	public R httpOnly(String name, boolean expected) {
		boolean isHttpOnly = getCookie(name).isHttpOnly();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " httpOnly";
			assertEquals(message, expected, isHttpOnly);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "Partitioned" attribute.
	 */
	public R partitioned(String name, boolean expected) {
		boolean isPartitioned = getCookie(name).isPartitioned();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " isPartitioned";
			assertEquals(message, expected, isPartitioned);
		});
		return this.responseSpec;
	}

	/**
	 * Assert a cookie's "SameSite" attribute.
	 */
	public R sameSite(String name, String expected) {
		String sameSite = getCookie(name).getSameSite();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " sameSite";
			assertEquals(message, expected, sameSite);
		});
		return this.responseSpec;
	}

	private ResponseCookie getCookie(String name) {
		ResponseCookie cookie = getResponseCookies().getFirst(name);
		if (cookie != null) {
			return cookie;
		}
		else {
			assertWithDiagnostics(() -> fail("No cookie with name '" + name + "'"));
		}
		throw new IllegalStateException("This code path should not be reachable");
	}

	private static String getMessage(String cookie) {
		return "Response cookie '" + cookie + "'";
	}
}
