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
public class StatusAssertions {

	private final HttpStatus httpStatus;

	private final WebTestClient.ResponseSpec responseSpec;


	StatusAssertions(HttpStatus status, WebTestClient.ResponseSpec responseSpec) {
		this.httpStatus = status;
		this.responseSpec = responseSpec;
	}


	/**
	 * Assert the response status as an {@link HttpStatus}.
	 */
	public WebTestClient.ResponseSpec isEqualTo(HttpStatus status) {
		assertEquals("Response status", status, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status as an integer.
	 */
	public WebTestClient.ResponseSpec isEqualTo(int status) {
		assertEquals("Response status", status, this.httpStatus.value());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.OK} (200).
	 */
	public WebTestClient.ResponseSpec isOk() {
		assertEquals("Status", HttpStatus.OK, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.CREATED} (201).
	 */
	public WebTestClient.ResponseSpec isCreated() {
		assertEquals("Status", HttpStatus.CREATED, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.ACCEPTED} (202).
	 */
	public WebTestClient.ResponseSpec isAccepted() {
		assertEquals("Status", HttpStatus.ACCEPTED, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NO_CONTENT} (204).
	 */
	public WebTestClient.ResponseSpec isNoContent() {
		assertEquals("Status", HttpStatus.NO_CONTENT, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.FOUND} (302).
	 */
	public WebTestClient.ResponseSpec isFound() {
		assertEquals("Status", HttpStatus.FOUND, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.SEE_OTHER} (303).
	 */
	public WebTestClient.ResponseSpec isSeeOther() {
		assertEquals("Status", HttpStatus.SEE_OTHER, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_MODIFIED} (304).
	 */
	public WebTestClient.ResponseSpec isNotModified() {
		assertEquals("Status", HttpStatus.NOT_MODIFIED, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.TEMPORARY_REDIRECT} (307).
	 */
	public WebTestClient.ResponseSpec isTemporaryRedirect() {
		assertEquals("Status", HttpStatus.TEMPORARY_REDIRECT, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.PERMANENT_REDIRECT} (308).
	 */
	public WebTestClient.ResponseSpec isPermanentRedirect() {
		assertEquals("Status", HttpStatus.PERMANENT_REDIRECT, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.BAD_REQUEST} (400).
	 */
	public WebTestClient.ResponseSpec isBadRequest() {
		assertEquals("Status", HttpStatus.BAD_REQUEST, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is {@code HttpStatus.NOT_FOUND} (404).
	 */
	public WebTestClient.ResponseSpec isNotFound() {
		assertEquals("Status", HttpStatus.NOT_FOUND, this.httpStatus);
		return this.responseSpec;
	}

	/**
	 * Assert the response error message.
	 */
	public WebTestClient.ResponseSpec reasonEquals(String reason) {
		assertEquals("Response status reason", reason, this.httpStatus.getReasonPhrase());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 1xx range.
	 */
	public WebTestClient.ResponseSpec is1xxInformational() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.INFORMATIONAL, this.httpStatus.series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 2xx range.
	 */
	public WebTestClient.ResponseSpec is2xxSuccessful() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SUCCESSFUL, this.httpStatus.series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 3xx range.
	 */
	public WebTestClient.ResponseSpec is3xxRedirection() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.REDIRECTION, this.httpStatus.series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 4xx range.
	 */
	public WebTestClient.ResponseSpec is4xxClientError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.CLIENT_ERROR, this.httpStatus.series());
		return this.responseSpec;
	}

	/**
	 * Assert the response status code is in the 5xx range.
	 */
	public WebTestClient.ResponseSpec is5xxServerError() {
		String message = "Range for response status value " + this.httpStatus;
		assertEquals(message, HttpStatus.Series.SERVER_ERROR, this.httpStatus.series());
		return this.responseSpec;
	}

}
