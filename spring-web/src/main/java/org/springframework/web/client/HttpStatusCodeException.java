/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * Abstract base class for exceptions based on an {@link HttpStatus}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 5696801857651587810L;


	private final HttpStatus statusCode;


	/**
	 * Construct a new instance with an {@link HttpStatus}.
	 * @param statusCode the status code
	 */
	protected HttpStatusCodeException(HttpStatus statusCode) {
		this(statusCode, statusCode.name(), null, null, null);
	}

	/**
	 * Construct a new instance with an {@link HttpStatus} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText) {
		this(statusCode, statusText, null, null, null);
	}

	/**
	 * Construct instance with an {@link HttpStatus}, status text, and content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.0.5
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(statusCode, statusText, null, responseBody, responseCharset);
	}

	/**
	 * Construct instance with an {@link HttpStatus}, status text, content, and
	 * a response charset.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers, may be {@code null}
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.1.2
	 */
	protected HttpStatusCodeException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(statusCode.value() + " " + statusText, statusCode.value(), statusText,
				responseHeaders, responseBody, responseCharset);
		this.statusCode = statusCode;
	}


	/**
	 * Return the HTTP status code.
	 */
	public HttpStatus getStatusCode() {
		return this.statusCode;
	}

}
