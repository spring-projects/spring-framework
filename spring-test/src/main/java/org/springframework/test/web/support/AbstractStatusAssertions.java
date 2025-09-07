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

import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.AssertionErrors;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

/**
 * Assertions on the response status.
 *
 * @author Rob Worsnop
 * @author Rossen Stoyanchev
 * @since 7.0
 * @param <E> the type of the exchange result
 * @param <R> the type of the response spec
 */
public abstract class AbstractStatusAssertions<E, R> {

	private final E exchangeResult;

	private final R responseSpec;


	protected AbstractStatusAssertions(E exchangeResult, R responseSpec) {
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
	 * Subclasses must implement this to provide access to the response status.
	 */
	protected abstract HttpStatusCode getStatus();

	/**
	 * Subclasses must implement this to assert with diagnostics.
	 */
	protected abstract void assertWithDiagnostics(Runnable assertion);


	/**
	 * Assert the response status as an {@link HttpStatusCode}.
	 */
	public R isEqualTo(HttpStatusCode status) {
		HttpStatusCode actual = getStatus();
		assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", status, actual));
		return this.responseSpec;
	}

	/**
	 * Assert the response status as an integer.
	 */
	public R isEqualTo(int status) {
		return isEqualTo(HttpStatusCode.valueOf(status));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public R isOk() {
		return assertStatusAndReturn(HttpStatus.OK);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public R isCreated() {
		return assertStatusAndReturn(HttpStatus.CREATED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public R isAccepted() {
		return assertStatusAndReturn(HttpStatus.ACCEPTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public R isNoContent() {
		return assertStatusAndReturn(HttpStatus.NO_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public R isFound() {
		return assertStatusAndReturn(HttpStatus.FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public R isSeeOther() {
		return assertStatusAndReturn(HttpStatus.SEE_OTHER);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public R isNotModified() {
		return assertStatusAndReturn(HttpStatus.NOT_MODIFIED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public R isTemporaryRedirect() {
		return assertStatusAndReturn(HttpStatus.TEMPORARY_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public R isPermanentRedirect() {
		return assertStatusAndReturn(HttpStatus.PERMANENT_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public R isBadRequest() {
		return assertStatusAndReturn(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
	 */
	public R isUnauthorized() {
		return assertStatusAndReturn(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
	 */
	public R isForbidden() {
		return assertStatusAndReturn(HttpStatus.FORBIDDEN);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public R isNotFound() {
		return assertStatusAndReturn(HttpStatus.NOT_FOUND);
	}

	/**
	 * Assert the response error message.
	 */
	public R reasonEquals(String reason) {
		String actual = getReasonPhrase(getStatus());
		assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Response status reason", reason, actual));
		return this.responseSpec;
	}

	private static String getReasonPhrase(HttpStatusCode statusCode) {
		if (statusCode instanceof HttpStatus status) {
			return status.getReasonPhrase();
		}
		else {
			return "";
		}
	}


	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public R is1xxInformational() {
		return assertSeriesAndReturn(HttpStatus.Series.INFORMATIONAL);
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public R is2xxSuccessful() {
		return assertSeriesAndReturn(HttpStatus.Series.SUCCESSFUL);
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public R is3xxRedirection() {
		return assertSeriesAndReturn(HttpStatus.Series.REDIRECTION);
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public R is4xxClientError() {
		return assertSeriesAndReturn(HttpStatus.Series.CLIENT_ERROR);
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public R is5xxServerError() {
		return assertSeriesAndReturn(HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * Match the response status value with a Hamcrest matcher.
	 * @param matcher the matcher to use
	 */
	public R value(Matcher<? super Integer> matcher) {
		int actual = getStatus().value();
		assertWithDiagnostics(() -> MatcherAssert.assertThat("Response status", actual, matcher));
		return this.responseSpec;
	}

	/**
	 * Consume the response status value as an integer.
	 * @param consumer the consumer to use
	 * @since 5.1
	 */
	public R value(Consumer<Integer> consumer) {
		int actual = getStatus().value();
		assertWithDiagnostics(() -> consumer.accept(actual));
		return this.responseSpec;
	}

	private R assertStatusAndReturn(HttpStatus expected) {
		assertNotNull("exchangeResult unexpectedly null", this.exchangeResult);
		HttpStatusCode actual = getStatus();
		assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", expected, actual));
		return this.responseSpec;
	}

	private R assertSeriesAndReturn(HttpStatus.Series expected) {
		HttpStatusCode status = getStatus();
		HttpStatus.Series series = HttpStatus.Series.resolve(status.value());
		assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Range for response status value " + status, expected, series));
		return this.responseSpec;
	}
}
