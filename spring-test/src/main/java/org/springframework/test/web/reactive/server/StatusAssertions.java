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
 * Assertions on the status of a response.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see ResponseAssertions#status()
 */
@SuppressWarnings("unused")
public class StatusAssertions<T> {

	private final ResponseAssertions<T> resultAssertions;

	private final HttpStatus httpStatus;


	StatusAssertions(HttpStatus status, ResponseAssertions<T> exchangeActions) {
		this.resultAssertions = exchangeActions;
		this.httpStatus = status;
	}


	/**
	 * Assert the response status as an {@link HttpStatus}.
	 */
	public ResponseAssertions<T> isEqualTo(HttpStatus status) {
		assertEquals("Response status", status, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status as an integer.
	 */
	public ResponseAssertions<T> isEqualTo(int status) {
		assertEquals("Response status", status, this.httpStatus.value());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public ResponseAssertions<T> isOk() {
		assertEquals("Status", HttpStatus.OK, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public ResponseAssertions<T> isCreated() {
		assertEquals("Status", HttpStatus.CREATED, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public ResponseAssertions<T> isAccepted() {
		assertEquals("Status", HttpStatus.ACCEPTED, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public ResponseAssertions<T> isNoContent() {
		assertEquals("Status", HttpStatus.NO_CONTENT, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public ResponseAssertions<T> isFound() {
		assertEquals("Status", HttpStatus.FOUND, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public ResponseAssertions<T> isSeeOther() {
		assertEquals("Status", HttpStatus.SEE_OTHER, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public ResponseAssertions<T> isNotModified() {
		assertEquals("Status", HttpStatus.NOT_MODIFIED, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public ResponseAssertions<T> isTemporaryRedirect() {
		assertEquals("Status", HttpStatus.TEMPORARY_REDIRECT, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public ResponseAssertions<T> isPermanentRedirect() {
		assertEquals("Status", HttpStatus.PERMANENT_REDIRECT, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public ResponseAssertions<T> isBadRequest() {
		assertEquals("Status", HttpStatus.BAD_REQUEST, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public ResponseAssertions<T> isNotFound() {
		assertEquals("Status", HttpStatus.NOT_FOUND, this.httpStatus);
		return this.resultAssertions;
	}

	/**
	 * Assert the response error message.
	 */
	public ResponseAssertions<T> reasonEquals(String reason) {
		assertEquals("Response status reason", reason, this.httpStatus.getReasonPhrase());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public ResponseAssertions<T> is1xxInformational() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.INFORMATIONAL, this.httpStatus.series());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public ResponseAssertions<T> is2xxSuccessful() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SUCCESSFUL, this.httpStatus.series());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public ResponseAssertions<T> is3xxRedirection() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.REDIRECTION, this.httpStatus.series());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public ResponseAssertions<T> is4xxClientError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.CLIENT_ERROR, this.httpStatus.series());
		return this.resultAssertions;
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public ResponseAssertions<T> is5xxServerError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SERVER_ERROR, this.httpStatus.series());
		return this.resultAssertions;
	}

}
