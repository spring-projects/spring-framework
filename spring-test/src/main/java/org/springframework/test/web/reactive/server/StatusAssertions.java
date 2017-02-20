/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.web.reactive.server;

import org.springframework.http.HttpStatus;

import static org.springframework.test.util.AssertionErrors.assertEquals;

/**
 * Assertions on the response status.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectStatus()
 */
@SuppressWarnings("unused")
public class StatusAssertions {

	private final ExchangeResult<?> exchangeResult;

	private final WebTestClient.ResponseSpec responseSpec;


	StatusAssertions(ExchangeResult<?> exchangeResult, WebTestClient.ResponseSpec responseSpec) {
		this.exchangeResult = exchangeResult;
		this.responseSpec = responseSpec;
	}


	/**
	 * Assert the response status as an {@link HttpStatus}.
	 */
	public WebTestClient.ResponseSpec isEqualTo(HttpStatus status) {
		assertEquals("Response status", status, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status as an integer.
	 */
	public WebTestClient.ResponseSpec isEqualTo(int status) {
		assertEquals("Response status", status, getStatus().value());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public WebTestClient.ResponseSpec isOk() {
		assertEquals("Status", HttpStatus.OK, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public WebTestClient.ResponseSpec isCreated() {
		assertEquals("Status", HttpStatus.CREATED, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public WebTestClient.ResponseSpec isAccepted() {
		assertEquals("Status", HttpStatus.ACCEPTED, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public WebTestClient.ResponseSpec isNoContent() {
		assertEquals("Status", HttpStatus.NO_CONTENT, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public WebTestClient.ResponseSpec isFound() {
		assertEquals("Status", HttpStatus.FOUND, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public WebTestClient.ResponseSpec isSeeOther() {
		assertEquals("Status", HttpStatus.SEE_OTHER, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public WebTestClient.ResponseSpec isNotModified() {
		assertEquals("Status", HttpStatus.NOT_MODIFIED, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public WebTestClient.ResponseSpec isTemporaryRedirect() {
		assertEquals("Status", HttpStatus.TEMPORARY_REDIRECT, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public WebTestClient.ResponseSpec isPermanentRedirect() {
		assertEquals("Status", HttpStatus.PERMANENT_REDIRECT, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public WebTestClient.ResponseSpec isBadRequest() {
		assertEquals("Status", HttpStatus.BAD_REQUEST, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.UNAUTHORIZED} (401).
	 */
	public WebTestClient.ResponseSpec isUnauthorized() {
		assertEquals("Status", HttpStatus.UNAUTHORIZED, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public WebTestClient.ResponseSpec isNotFound() {
		assertEquals("Status", HttpStatus.NOT_FOUND, getStatus());
		return this.responseSpec;
	}

	/**
	 * Assert the response error message.
	 */
	public WebTestClient.ResponseSpec reasonEquals(String reason) {
		assertEquals("Response status reason", reason, getStatus().getReasonPhrase());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public WebTestClient.ResponseSpec is1xxInformational() {
		String message = "Range for response status value " + getStatus();
		assertEquals(message, HttpStatus.Series.INFORMATIONAL, getStatus().series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public WebTestClient.ResponseSpec is2xxSuccessful() {
		String message = "Range for response status value " + getStatus();
		assertEquals(message, HttpStatus.Series.SUCCESSFUL, getStatus().series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public WebTestClient.ResponseSpec is3xxRedirection() {
		String message = "Range for response status value " + getStatus();
		assertEquals(message, HttpStatus.Series.REDIRECTION, getStatus().series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public WebTestClient.ResponseSpec is4xxClientError() {
		String message = "Range for response status value " + getStatus();
		assertEquals(message, HttpStatus.Series.CLIENT_ERROR, getStatus().series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public WebTestClient.ResponseSpec is5xxServerError() {
		String message = "Range for response status value " + getStatus();
		assertEquals(message, HttpStatus.Series.SERVER_ERROR, getStatus().series());
		return this.responseSpec;
	}


	// Private methods

	private HttpStatus getStatus() {
		return this.exchangeResult.getStatus();
	}

}
