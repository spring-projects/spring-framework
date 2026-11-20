/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
