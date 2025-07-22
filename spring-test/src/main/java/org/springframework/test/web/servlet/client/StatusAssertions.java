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

package org.springframework.test.web.servlet.client;

import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.AssertionErrors;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

/**
 * Assertions on the response status.
 *
 * @author Rob Worsnop
 *
 * @see ResponseSpec#expectStatus()
 */
public class StatusAssertions {

	private final ExchangeResult exchangeResult;

	private final ResponseSpec responseSpec;

	public StatusAssertions(ExchangeResult exchangeResult, ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * Assert the response status as an {@link HttpStatusCode}.
	 */
	public RestTestClient.ResponseSpec isEqualTo(HttpStatusCode status) {
		HttpStatusCode actual = this.exchangeResult.getStatus();
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", status, actual));
		return this.responseSpec;
	}

	/**
	 * Assert the response status as an integer.
	 */
	public RestTestClient.ResponseSpec isEqualTo(int status) {
		return isEqualTo(HttpStatusCode.valueOf(status));
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public RestTestClient.ResponseSpec isOk() {
		return assertStatusAndReturn(HttpStatus.OK);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public RestTestClient.ResponseSpec isCreated() {
		return assertStatusAndReturn(HttpStatus.CREATED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public RestTestClient.ResponseSpec isAccepted() {
		return assertStatusAndReturn(HttpStatus.ACCEPTED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public RestTestClient.ResponseSpec isNoContent() {
		return assertStatusAndReturn(HttpStatus.NO_CONTENT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public RestTestClient.ResponseSpec isFound() {
		return assertStatusAndReturn(HttpStatus.FOUND);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public RestTestClient.ResponseSpec isSeeOther() {
		return assertStatusAndReturn(HttpStatus.SEE_OTHER);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public RestTestClient.ResponseSpec isNotModified() {
		return assertStatusAndReturn(HttpStatus.NOT_MODIFIED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public RestTestClient.ResponseSpec isTemporaryRedirect() {
		return assertStatusAndReturn(HttpStatus.TEMPORARY_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public RestTestClient.ResponseSpec isPermanentRedirect() {
		return assertStatusAndReturn(HttpStatus.PERMANENT_REDIRECT);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public RestTestClient.ResponseSpec isBadRequest() {
		return assertStatusAndReturn(HttpStatus.BAD_REQUEST);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
	 */
	public RestTestClient.ResponseSpec isUnauthorized() {
		return assertStatusAndReturn(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FORBIDDEN} (403).
	 * @since 5.0.2
	 */
	public RestTestClient.ResponseSpec isForbidden() {
		return assertStatusAndReturn(HttpStatus.FORBIDDEN);
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public RestTestClient.ResponseSpec isNotFound() {
		return assertStatusAndReturn(HttpStatus.NOT_FOUND);
	}

	/**
	 * Assert the response error message.
	 */
	public RestTestClient.ResponseSpec reasonEquals(String reason) {
		String actual = getReasonPhrase(this.exchangeResult.getStatus());
		this.exchangeResult.assertWithDiagnostics(() ->
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
	public RestTestClient.ResponseSpec is1xxInformational() {
		return assertSeriesAndReturn(HttpStatus.Series.INFORMATIONAL);
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public RestTestClient.ResponseSpec is2xxSuccessful() {
		return assertSeriesAndReturn(HttpStatus.Series.SUCCESSFUL);
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public RestTestClient.ResponseSpec is3xxRedirection() {
		return assertSeriesAndReturn(HttpStatus.Series.REDIRECTION);
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public RestTestClient.ResponseSpec is4xxClientError() {
		return assertSeriesAndReturn(HttpStatus.Series.CLIENT_ERROR);
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public RestTestClient.ResponseSpec is5xxServerError() {
		return assertSeriesAndReturn(HttpStatus.Series.SERVER_ERROR);
	}

	/**
	 * Match the response status value with a Hamcrest matcher.
	 * @param matcher the matcher to use
	 * @since 5.1
	 */
	public RestTestClient.ResponseSpec value(Matcher<? super Integer> matcher) {
		int actual = this.exchangeResult.getStatus().value();
		this.exchangeResult.assertWithDiagnostics(() -> MatcherAssert.assertThat("Response status", actual, matcher));
		return this.responseSpec;
	}

	/**
	 * Consume the response status value as an integer.
	 * @param consumer the consumer to use
	 * @since 5.1
	 */
	public RestTestClient.ResponseSpec value(Consumer<Integer> consumer) {
		int actual = this.exchangeResult.getStatus().value();
		this.exchangeResult.assertWithDiagnostics(() -> consumer.accept(actual));
		return this.responseSpec;
	}


	private ResponseSpec assertStatusAndReturn(HttpStatus expected) {
		assertNotNull("exchangeResult unexpectedly null", this.exchangeResult);
		HttpStatusCode actual = this.exchangeResult.getStatus();
		this.exchangeResult.assertWithDiagnostics(() -> AssertionErrors.assertEquals("Status", expected, actual));
		return this.responseSpec;
	}

	private RestTestClient.ResponseSpec assertSeriesAndReturn(HttpStatus.Series expected) {
		HttpStatusCode status = this.exchangeResult.getStatus();
		HttpStatus.Series series = HttpStatus.Series.resolve(status.value());
		this.exchangeResult.assertWithDiagnostics(() ->
				AssertionErrors.assertEquals("Range for response status value " + status, expected, series));
		return this.responseSpec;
	}
}
