/*
 * Copyright 2002-2016 the original author or authors.
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
	 * Construct a new instance of {@code HttpClientErrorException} based on
	 * an {@link HttpStatus}.
	 * @param statusCode the status code
	 */
	public HttpClientErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * Construct a new instance of {@code HttpClientErrorException} based on
	 * an {@link HttpStatus} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * Construct a new instance of {@code HttpClientErrorException} based on
	 * an {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(statusCode, statusText, responseBody, responseCharset);
	}

	/**
	 * Construct a new instance of {@code HttpClientErrorException} based on
	 * an {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers (may be {@code null})
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 * @since 3.1.2
	 */
	public HttpClientErrorException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	/**
	 * Exception thrown when an HTTP 400 Bad Request is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class BadRequest extends HttpClientErrorException {

		private static final long serialVersionUID = -2064198172908428197L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.BadRequest}.
		 */
		public BadRequest() {
			super(HttpStatus.BAD_REQUEST);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.BadRequest} based on status text.
		 * @param statusText the status text
		 */
		public BadRequest(String statusText) {
			super(HttpStatus.BAD_REQUEST, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.BadRequest} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public BadRequest(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.BAD_REQUEST, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.BadRequest} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public BadRequest(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.BAD_REQUEST, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 401 Unauthorized is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class Unauthorized extends HttpClientErrorException {

		private static final long serialVersionUID = 2770517013134530298L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Unauthorized}.
		 */
		public Unauthorized() {
			super(HttpStatus.UNAUTHORIZED);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Unauthorized} based on status text.
		 * @param statusText the status text
		 */
		public Unauthorized(String statusText) {
			super(HttpStatus.UNAUTHORIZED, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Unauthorized} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Unauthorized(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNAUTHORIZED, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Unauthorized} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Unauthorized(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNAUTHORIZED, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 403 Forbidden is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class Forbidden extends HttpClientErrorException {

		private static final long serialVersionUID = 620402597011417919L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Forbidden}.
		 */
		public Forbidden() {
			super(HttpStatus.FORBIDDEN);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Forbidden} based on status text.
		 * @param statusText the status text
		 */
		public Forbidden(String statusText) {
			super(HttpStatus.FORBIDDEN, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Forbidden} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Forbidden(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.FORBIDDEN, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Forbidden} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Forbidden(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.FORBIDDEN, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 404 Not Found is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class NotFound extends HttpClientErrorException {

		private static final long serialVersionUID = -9150078287238394669L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotFound}.
		 */
		public NotFound() {
			super(HttpStatus.NOT_FOUND);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotFound} based on status text.
		 * @param statusText the status text
		 */
		public NotFound(String statusText) {
			super(HttpStatus.NOT_FOUND, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotFound} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotFound(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_FOUND, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotFound} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotFound(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_FOUND, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 405 Method Not Allowed is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class MethodNotAllowed extends HttpClientErrorException {

		private static final long serialVersionUID = -1485854208191929937L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.MethodNotAllowed}.
		 */
		public MethodNotAllowed() {
			super(HttpStatus.METHOD_NOT_ALLOWED);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.MethodNotAllowed} based on status text.
		 * @param statusText the status text
		 */
		public MethodNotAllowed(String statusText) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.MethodNotAllowed} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public MethodNotAllowed(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.MethodNotAllowed} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public MethodNotAllowed(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.METHOD_NOT_ALLOWED, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 406 Not Acceptable is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class NotAcceptable extends HttpClientErrorException {

		private static final long serialVersionUID = -1762209525396296759L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotAcceptable}.
		 */
		public NotAcceptable() {
			super(HttpStatus.NOT_ACCEPTABLE);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotAcceptable} based on status text.
		 * @param statusText the status text
		 */
		public NotAcceptable(String statusText) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotAcceptable} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotAcceptable(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.NotAcceptable} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotAcceptable(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_ACCEPTABLE, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 409 Conflict is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class Conflict extends HttpClientErrorException {

		private static final long serialVersionUID = -147527825450228693L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Conflict}.
		 */
		public Conflict() {
			super(HttpStatus.CONFLICT);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Conflict} based on status text.
		 * @param statusText the status text
		 */
		public Conflict(String statusText) {
			super(HttpStatus.CONFLICT, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Conflict} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Conflict(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.CONFLICT, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Conflict} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Conflict(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.CONFLICT, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 410 Gone is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class Gone extends HttpClientErrorException {

		private static final long serialVersionUID = -147527825450228693L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Gone}.
		 */
		public Gone() {
			super(HttpStatus.GONE);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Gone} based on status text.
		 * @param statusText the status text
		 */
		public Gone(String statusText) {
			super(HttpStatus.GONE, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Gone} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Gone(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.GONE, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.Gone} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public Gone(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.GONE, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 415 Unsupported Media Type is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class UnsupportedMediaType extends HttpClientErrorException {

		private static final long serialVersionUID = -7894170475662610655L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnsupportedMediaType}.
		 */
		public UnsupportedMediaType() {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnsupportedMediaType} based on status text.
		 * @param statusText the status text
		 */
		public UnsupportedMediaType(String statusText) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnsupportedMediaType} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public UnsupportedMediaType(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnsupportedMediaType} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public UnsupportedMediaType(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 422 Unprocessable Entity is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class UnprocessableEntity extends HttpClientErrorException {

		private static final long serialVersionUID = 147931406869809016L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnprocessableEntity}.
		 */
		public UnprocessableEntity() {
			super(HttpStatus.UNPROCESSABLE_ENTITY);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnprocessableEntity} based on status text.
		 * @param statusText the status text
		 */
		public UnprocessableEntity(String statusText) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnprocessableEntity} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public UnprocessableEntity(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.UnprocessableEntity} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public UnprocessableEntity(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.UNPROCESSABLE_ENTITY, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 429 Too Many Requests is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class TooManyRequests extends HttpClientErrorException {

		private static final long serialVersionUID = -7180196215964324224L;

		/**
		 * Construct a new instance of {@code HttpClientErrorException.TooManyRequests}.
		 */
		public TooManyRequests() {
			super(HttpStatus.TOO_MANY_REQUESTS);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.TooManyRequests} based on status text.
		 * @param statusText the status text
		 */
		public TooManyRequests(String statusText) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.TooManyRequests} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public TooManyRequests(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpClientErrorException.TooManyRequests} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public TooManyRequests(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.TOO_MANY_REQUESTS, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

}
