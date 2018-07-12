/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * Exceptions that contain actual HTTP response data.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public class WebClientResponseException extends WebClientException {

	private static final long serialVersionUID = 4127543205414951611L;


	private final int statusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final HttpHeaders headers;

	private final Charset responseCharset;


	/**
	 * Construct a new instance of with the given response data.
	 * @param message the exception message
	 * @param statusCode the raw status code value
	 * @param statusText the status text
	 * @param headers the response headers (may be {@code null})
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] responseBody,
			@Nullable Charset responseCharset) {

		super(message);

		this.statusCode = statusCode;
		this.statusText = statusText;
		this.headers = (headers != null ? headers : HttpHeaders.EMPTY);
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (responseCharset != null ? responseCharset : StandardCharsets.ISO_8859_1);
	}


	/**
	 * Return the HTTP status code value.
	 * @throws IllegalArgumentException in case of an unknown HTTP status code
	 */
	public HttpStatus getStatusCode() {
		return HttpStatus.valueOf(this.statusCode);
	}

	/**
	 * Return the raw HTTP status code value.
	 */
	public int getRawStatusCode() {
		return this.statusCode;
	}

	/**
	 * Return the HTTP status text.
	 */
	public String getStatusText() {
		return this.statusText;
	}

	/**
	 * Return the HTTP response headers.
	 */
	public HttpHeaders getHeaders() {
		return this.headers;
	}

	/**
	 * Return the response body as a byte array.
	 */
	public byte[] getResponseBodyAsByteArray() {
		return this.responseBody;
	}

	/**
	 * Return the response body as a string.
	 */
	public String getResponseBodyAsString() {
		return new String(this.responseBody, this.responseCharset);
	}

}
