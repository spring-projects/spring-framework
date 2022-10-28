/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Abstract base class for exceptions based on an {@link HttpStatusCode}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public abstract class HttpStatusCodeException extends RestClientResponseException {

	private static final long serialVersionUID = 5696801857651587810L;


	/**
	 * Construct a new instance with an {@link HttpStatusCode}.
	 * @param statusCode the status code
	 */
	protected HttpStatusCodeException(HttpStatusCode statusCode) {
		this(statusCode, name(statusCode), null, null, null);
	}

	private static String name(HttpStatusCode statusCode) {
		if (statusCode instanceof HttpStatus status) {
			return status.name();
		}
		else {
			return "";
		}
	}

	/**
	 * Construct a new instance with an {@link HttpStatusCode} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText) {
		this(statusCode, statusText, null, null, null);
	}

	/**
	 * Construct instance with an {@link HttpStatusCode}, status text, and content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.0.5
	 */
	protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(statusCode, statusText, null, responseBody, responseCharset);
	}

	/**
	 * Construct instance with an {@link HttpStatusCode}, status text, content, and
	 * a response charset.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers, may be {@code null}
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 3.1.2
	 */
	protected HttpStatusCodeException(HttpStatusCode statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		this(getMessage(statusCode, statusText),
				statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	/**
	 * Construct instance with an {@link HttpStatusCode}, status text, content, and
	 * a response charset.
	 * @param message the exception message
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers, may be {@code null}
	 * @param responseBody the response body content, may be {@code null}
	 * @param responseCharset the response body charset, may be {@code null}
	 * @since 5.2.2
	 */
	protected HttpStatusCodeException(String message, HttpStatusCode statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(message, statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	private static String getMessage(HttpStatusCode statusCode, String statusText) {
		if (!StringUtils.hasLength(statusText) && statusCode instanceof HttpStatus status) {
			statusText = status.getReasonPhrase();
		}
		return statusCode.value() + " " + statusText;
	}

}
