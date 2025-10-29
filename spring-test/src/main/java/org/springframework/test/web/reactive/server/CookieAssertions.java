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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import org.hamcrest.Matcher;

import org.springframework.http.ResponseCookie;
import org.springframework.test.web.support.AbstractCookieAssertions;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Assertions on cookies of the response.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @since 5.3
 */
public class CookieAssertions extends AbstractCookieAssertions<ExchangeResult, WebTestClient.ResponseSpec> {


	CookieAssertions(ExchangeResult exchangeResult, WebTestClient.ResponseSpec responseSpec) {
		super(exchangeResult, responseSpec);
	}


	@Override
	protected MultiValueMap<String, ResponseCookie> getResponseCookies() {
		return getExchangeResult().getResponseCookies();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}


	/**
	 * Assert the value of the response cookie with the given name with a Hamcrest
	 * {@link Matcher}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getCookie(name).getValue();
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertThat(message, value, matcher);
		});
		return getResponseSpec();
	}


	/**
	 * Assert a cookie's "Max-Age" attribute with a Hamcrest {@link Matcher}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec maxAge(String name, Matcher<? super Long> matcher) {
		long maxAge = getCookie(name).getMaxAge().getSeconds();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " maxAge";
			assertThat(message, maxAge, matcher);
		});
		return getResponseSpec();
	}

	/**
	 * Assert a cookie's "Path" attribute with a Hamcrest {@link Matcher}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec path(String name, Matcher<? super String> matcher) {
		String path = getCookie(name).getPath();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " path";
			assertThat(message, path, matcher);
		});
		return getResponseSpec();
	}

	/**
	 * Assert a cookie's "Domain" attribute with a Hamcrest {@link Matcher}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec domain(String name, Matcher<? super String> matcher) {
		String domain = getCookie(name).getDomain();
		assertWithDiagnostics(() -> {
			String message = getMessage(name) + " domain";
			assertThat(message, domain, matcher);
		});
		return getResponseSpec();
	}


}
