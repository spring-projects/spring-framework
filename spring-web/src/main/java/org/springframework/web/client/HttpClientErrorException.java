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

package org.springframework.web.client;

import java.nio.charset.Charset;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

/**
 * Exception thrown when an HTTP 4xx is received.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see DefaultResponseErrorHandler
 */
public class HttpClientErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = 5177019431887513952L;


	/**
	 * Constructor with a status code only.
	 */
	public HttpClientErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * Constructor with a status code and status text.
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * Constructor with a status code and status text, and content.
	 */
	public HttpClientErrorException(
			HttpStatus statusCode, String statusText, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(statusCode, statusText, body, responseCharset);
	}

	/**
	 * Constructor with a status code and status text, headers, and content.
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(statusCode, statusText, headers, body, responseCharset);
	}


	// Subclasses for specific HTTP status codes

	/**
	 * {@link HttpClientErrorException} for status HTTP 400 Bad Request.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadRequest extends HttpClientErrorException {

		BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 401 Unauthorized.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Unauthorized extends HttpClientErrorException {

		Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 403 Forbidden.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Forbidden extends HttpClientErrorException {

		Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.FORBIDDEN, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 404 Not Found.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotFound extends HttpClientErrorException {

		NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_FOUND, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 405 Method Not Allowed.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class MethodNotAllowed extends HttpClientErrorException {

		MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 406 Not Acceptable.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotAcceptable extends HttpClientErrorException {

		NotAcceptable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 409 Conflict.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Conflict extends HttpClientErrorException {

		Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.CONFLICT, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 410 Gone.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Gone extends HttpClientErrorException {

		Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.GONE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 415 Unsupported Media Type.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnsupportedMediaType extends HttpClientErrorException {

		UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 422 Unprocessable Entity.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnprocessableEntity extends HttpClientErrorException {

		UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 429 Too Many Requests.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class TooManyRequests extends HttpClientErrorException {

		TooManyRequests(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
		}
	}

}
