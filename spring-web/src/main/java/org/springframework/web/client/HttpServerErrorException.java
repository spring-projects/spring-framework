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
 * Exception thrown when an HTTP 5xx is received.
 *
 * @author Arjen Poutsma
 * @since 3.0
 * @see DefaultResponseErrorHandler
 */
public class HttpServerErrorException extends HttpStatusCodeException {

	private static final long serialVersionUID = -2915754006618138282L;


	/**
	 * Construct a new instance of {@code HttpServerErrorException} based on
	 * an {@link HttpStatus}.
	 * @param statusCode the status code
	 */
	public HttpServerErrorException(HttpStatus statusCode) {
		super(statusCode);
	}

	/**
	 * Construct a new instance of {@code HttpServerErrorException} based on
	 * an {@link HttpStatus} and status text.
	 * @param statusCode the status code
	 * @param statusText the status text
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText) {
		super(statusCode, statusText);
	}

	/**
	 * Construct a new instance of {@code HttpServerErrorException} based on
	 * an {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 * @since 3.0.5
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText,
			@Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(statusCode, statusText, responseBody, responseCharset);
	}

	/**
	 * Construct a new instance of {@code HttpServerErrorException} based on
	 * an {@link HttpStatus}, status text, and response body content.
	 * @param statusCode the status code
	 * @param statusText the status text
	 * @param responseHeaders the response headers (may be {@code null})
	 * @param responseBody the response body content (may be {@code null})
	 * @param responseCharset the response body charset (may be {@code null})
	 * @since 3.1.2
	 */
	public HttpServerErrorException(HttpStatus statusCode, String statusText,
			@Nullable HttpHeaders responseHeaders, @Nullable byte[] responseBody, @Nullable Charset responseCharset) {

		super(statusCode, statusText, responseHeaders, responseBody, responseCharset);
	}

	/**
	 * Exception thrown when an HTTP 500 Internal Server Error is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class InternalServerError extends HttpServerErrorException {

		private static final long serialVersionUID = -9078091996219553426L;

		/**
		 * Construct a new instance of {@code HttpServerErrorException.InternalServerError}.
		 */
		public InternalServerError() {
			super(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.InternalServerError} based on status text.
		 * @param statusText the status text
		 */
		public InternalServerError(String statusText) {
			super(HttpStatus.INTERNAL_SERVER_ERROR, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.InternalServerError} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public InternalServerError(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.InternalServerError} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public InternalServerError(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.INTERNAL_SERVER_ERROR, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 501 Not Implemented is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class NotImplemented extends HttpServerErrorException {

		private static final long serialVersionUID = -8858888941453536625L;

		/**
		 * Construct a new instance of {@code HttpServerErrorException.NotImplemented}.
		 */
		public NotImplemented() {
			super(HttpStatus.NOT_IMPLEMENTED);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.NotImplemented} based on status text.
		 * @param statusText the status text
		 */
		public NotImplemented(String statusText) {
			super(HttpStatus.NOT_IMPLEMENTED, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.NotImplemented} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotImplemented(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_IMPLEMENTED, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.NotImplemented} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public NotImplemented(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.NOT_IMPLEMENTED, statusText, responseHeaders, responseBody, responseCharset);
		}

	}


	/**
	 * Exception thrown when an HTTP 502 Bad Gateway is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class BadGateway extends HttpServerErrorException {

		private static final long serialVersionUID = -8989300848677585487L;

		/**
		 * Construct a new instance of {@code HttpServerErrorException.BadGateway}.
		 */
		public BadGateway() {
			super(HttpStatus.BAD_GATEWAY);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.BadGateway} based on status text.
		 * @param statusText the status text
		 */
		public BadGateway(String statusText) {
			super(HttpStatus.BAD_GATEWAY, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.BadGateway} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public BadGateway(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.BAD_GATEWAY, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.BadGateway} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public BadGateway(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.BAD_GATEWAY, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 503 Service Unavailable is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class ServiceUnavailable extends HttpServerErrorException {

		private static final long serialVersionUID = 8777931838369402139L;

		/**
		 * Construct a new instance of {@code HttpServerErrorException.ServiceUnavailable}.
		 */
		public ServiceUnavailable() {
			super(HttpStatus.SERVICE_UNAVAILABLE);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.ServiceUnavailable} based on status text.
		 * @param statusText the status text
		 */
		public ServiceUnavailable(String statusText) {
			super(HttpStatus.SERVICE_UNAVAILABLE, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.ServiceUnavailable} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public ServiceUnavailable(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.SERVICE_UNAVAILABLE, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.ServiceUnavailable} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public ServiceUnavailable(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.SERVICE_UNAVAILABLE, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

	/**
	 * Exception thrown when an HTTP 504 Gateway Timeout is received.
	 *
	 * @since 5.1
	 * @see DefaultResponseErrorHandler
	 */
	public static class GatewayTimeout extends HttpServerErrorException {

		private static final long serialVersionUID = -7460116254256085095L;

		/**
		 * Construct a new instance of {@code HttpServerErrorException.GatewayTimeout}.
		 */
		public GatewayTimeout() {
			super(HttpStatus.GATEWAY_TIMEOUT);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.GatewayTimeout} based on status text.
		 * @param statusText the status text
		 */
		public GatewayTimeout(String statusText) {
			super(HttpStatus.GATEWAY_TIMEOUT, statusText);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.GatewayTimeout} based status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public GatewayTimeout(String statusText, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.GATEWAY_TIMEOUT, statusText, responseBody, responseCharset);
		}

		/**
		 * Construct a new instance of {@code HttpServerErrorException.GatewayTimeout} based on status text
		 * and response body content.
		 * @param statusText the status text
		 * @param responseHeaders the response headers (may be {@code null})
		 * @param responseBody the response body content (may be {@code null})
		 * @param responseCharset the response body charset (may be {@code null})
		 */
		public GatewayTimeout(String statusText, HttpHeaders responseHeaders, byte[] responseBody, Charset responseCharset) {
			super(HttpStatus.GATEWAY_TIMEOUT, statusText, responseHeaders, responseBody, responseCharset);
		}

	}

}
