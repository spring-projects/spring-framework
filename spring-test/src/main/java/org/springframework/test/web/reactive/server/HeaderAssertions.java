/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.util.List;
import java.util.function.Consumer;

import org.hamcrest.Matcher;

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.support.AbstractHeaderAssertions;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Assertions on headers of the response.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Rob Worsnop
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions extends AbstractHeaderAssertions<ExchangeResult, WebTestClient.ResponseSpec> {


	HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		super(result, spec);
	}


	@Override
	protected HttpHeaders getResponseHeaders() {
		return getExchangeResult().getResponseHeaders();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}



	/**
	 * Assert the first value of the response header with a Hamcrest {@link Matcher}.
	 * @param name the header name
	 * @param matcher the matcher to use
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec value(String name, Matcher<? super String> matcher) {
		String value = getResponseHeaders().getFirst(name);
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertThat(message, value, matcher);
		});
		return getResponseSpec();
	}

	/**
	 * Assert all values of the response header with a Hamcrest {@link Matcher}.
	 * @param name the header name
	 * @param matcher the matcher to use
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec values(String name, Matcher<? super Iterable<String>> matcher) {
		List<String> values = getResponseHeaders().get(name);
		assertWithDiagnostics(() -> {
			String message = getMessage(name);
			assertThat(message, values, matcher);
		});
		return getResponseSpec();
	}

}
