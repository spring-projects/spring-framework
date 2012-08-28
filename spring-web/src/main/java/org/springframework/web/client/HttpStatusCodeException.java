/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.client;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Abstract base class for exceptions based on an {@link HttpStatus}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.0
 */
public abstract class HttpStatusCodeException extends RestClientException {

	private static final long serialVersionUID = -5807494703720513267L;

	private static final String DEFAULT_CHARSET = "ISO-8859-1";

	private final HttpStatus statusCode;

	private final String statusText;

	private final byte[] responseBody;

	private final HttpHeaders responseHeaders;

	private final String responseCharset;


	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on an
	 * {@link HttpStatus}.
	 * @param statusCode the status code
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		this(statusCode, statusCode.name(), null, null, null);
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on an
	 * {@link HttpStatus} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		this(statusCode, statusText, null, null, null);
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on an
	 * {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.0.5
	 */
	protected HttpStatusCodeException(HttpStatus statusCode,
			String statusText,
			byte[] responseBody,
			Charset responseCharset) {
		this(statusCode, statusText, null, responseBody, responseCharset);
	}

	/**
	 * Construct a new instance of {@code HttpStatusCodeException} based on an
	 * {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers, may be {@code null}
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.1.2
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
		super(statusCode.value() + " " + statusText);
		this.statusCode = statusCode;
		this.statusText = statusText;
		this.responseHeaders = responseHeaders;
		this.responseBody = responseBody != null ? responseBody : new byte[0];
		this.responseCharset = responseCharset != null ? responseCharset.name() : DEFAULT_CHARSET;
	}


	/**
	 * Return the HTTP status code.
	 */
	public HttpStatus getStatusCode() {
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
	 * @since 3.1.2
	 */
	public HttpHeaders getResponseHeaders() {
		return this.responseHeaders;
	}

	/**
	 * Return the response body as a byte array.
	 *
	 * @since 3.0.5
	 */
	public byte[] getResponseBodyAsByteArray() {
		return responseBody;
	}

	/**
	 * Return the response body as a string.
	 * @since 3.0.5
	 */
	public String getResponseBodyAsString() {
		try {
			return new String(responseBody, responseCharset);
		}
		catch (UnsupportedEncodingException ex) {
			// should not occur
			throw new InternalError(ex.getMessage());
		}
	}

}
