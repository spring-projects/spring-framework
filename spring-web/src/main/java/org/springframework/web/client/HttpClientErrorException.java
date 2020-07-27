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

	/**
	 * Constructor with a status code and status text, headers, and content,
	 * and an prepared message.
	 * @since 5.2.2
	 */
	public HttpClientErrorException(String message, HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset responseCharset) {

		super(message, statusCode, statusText, headers, body, responseCharset);
	}


	/**
	 * Create {@code HttpClientErrorException} or an HTTP status specific sub-class.
	 * @since 5.1
	 */
	public static HttpClientErrorException create(
			HttpStatus statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(null, statusCode, statusText, headers, body, charset);
	}

	/**
	 * Variant of {@link #create(HttpStatus, String, HttpHeaders, byte[], Charset)}
	 * with an optional prepared message.
	 * @since 5.2.2
	 */
	public static HttpClientErrorException create(@Nullable String message, HttpStatus statusCode,
			String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		switch (statusCode) {
			case BAD_REQUEST:
				return message != null ?
						new HttpClientErrorException.BadRequest(message, statusText, headers, body, charset) :
						new HttpClientErrorException.BadRequest(statusText, headers, body, charset);
			case UNAUTHORIZED:
				return message != null ?
						new HttpClientErrorException.Unauthorized(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Unauthorized(statusText, headers, body, charset);
			case FORBIDDEN:
				return message != null ?
						new HttpClientErrorException.Forbidden(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Forbidden(statusText, headers, body, charset);
			case NOT_FOUND:
				return message != null ?
						new HttpClientErrorException.NotFound(message, statusText, headers, body, charset) :
						new HttpClientErrorException.NotFound(statusText, headers, body, charset);
			case METHOD_NOT_ALLOWED:
				return message != null ?
						new HttpClientErrorException.MethodNotAllowed(message, statusText, headers, body, charset) :
						new HttpClientErrorException.MethodNotAllowed(statusText, headers, body, charset);
			case NOT_ACCEPTABLE:
				return message != null ?
						new HttpClientErrorException.NotAcceptable(message, statusText, headers, body, charset) :
						new HttpClientErrorException.NotAcceptable(statusText, headers, body, charset);
			case CONFLICT:
				return message != null ?
						new HttpClientErrorException.Conflict(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Conflict(statusText, headers, body, charset);
			case GONE:
				return message != null ?
						new HttpClientErrorException.Gone(message, statusText, headers, body, charset) :
						new HttpClientErrorException.Gone(statusText, headers, body, charset);
			case UNSUPPORTED_MEDIA_TYPE:
				return message != null ?
						new HttpClientErrorException.UnsupportedMediaType(message, statusText, headers, body, charset) :
						new HttpClientErrorException.UnsupportedMediaType(statusText, headers, body, charset);
			case TOO_MANY_REQUESTS:
				return message != null ?
						new HttpClientErrorException.TooManyRequests(message, statusText, headers, body, charset) :
						new HttpClientErrorException.TooManyRequests(statusText, headers, body, charset);
			case UNPROCESSABLE_ENTITY:
				return message != null ?
						new HttpClientErrorException.UnprocessableEntity(message, statusText, headers, body, charset) :
						new HttpClientErrorException.UnprocessableEntity(statusText, headers, body, charset);
			default:
				return message != null ?
						new HttpClientErrorException(message, statusCode, statusText, headers, body, charset) :
						new HttpClientErrorException(statusCode, statusText, headers, body, charset);
		}
	}


	// Subclasses for specific HTTP status codes

	/**
	 * {@link HttpClientErrorException} for status HTTP 400 Bad Request.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class BadRequest extends HttpClientErrorException {

		private BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
		}

		private BadRequest(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.BAD_REQUEST, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 401 Unauthorized.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Unauthorized extends HttpClientErrorException {

		private Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
		}

		private Unauthorized(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNAUTHORIZED, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 403 Forbidden.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Forbidden extends HttpClientErrorException {

		private Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.FORBIDDEN, statusText, headers, body, charset);
		}

		private Forbidden(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.FORBIDDEN, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 404 Not Found.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotFound extends HttpClientErrorException {

		private NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_FOUND, statusText, headers, body, charset);
		}

		private NotFound(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_FOUND, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 405 Method Not Allowed.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class MethodNotAllowed extends HttpClientErrorException {

		private MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
		}

		private MethodNotAllowed(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.METHOD_NOT_ALLOWED, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 406 Not Acceptable.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class NotAcceptable extends HttpClientErrorException {

		private NotAcceptable(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
		}

		private NotAcceptable(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.NOT_ACCEPTABLE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 409 Conflict.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Conflict extends HttpClientErrorException {

		private Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.CONFLICT, statusText, headers, body, charset);
		}

		private Conflict(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(message, HttpStatus.CONFLICT, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 410 Gone.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class Gone extends HttpClientErrorException {

		private Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.GONE, statusText, headers, body, charset);
		}

		private Gone(String message, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(message, HttpStatus.GONE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 415 Unsupported Media Type.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class UnsupportedMediaType extends HttpClientErrorException {

		private UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
		}

		private UnsupportedMediaType(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 422 Unprocessable Entity.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class UnprocessableEntity extends HttpClientErrorException {

		private UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
		}

		private UnprocessableEntity(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.UNPROCESSABLE_ENTITY, statusText, headers, body, charset);
		}
	}

	/**
	 * {@link HttpClientErrorException} for status HTTP 429 Too Many Requests.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static final class TooManyRequests extends HttpClientErrorException {

		private TooManyRequests(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
		}

		private TooManyRequests(String message, String statusText,
				HttpHeaders headers, byte[] body, @Nullable Charset charset) {

			super(message, HttpStatus.TOO_MANY_REQUESTS, statusText, headers, body, charset);
		}
	}

}
