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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
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

	@Nullable
	private final HttpRequest request;


	/**
	 * Constructor with response data only, and a default message.
	 * @since 5.1
	 */
	public WebClientResponseException(int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset) {

		this(statusCode, statusText, headers, body, charset, null);
	}

	/**
	 * Constructor with response data only, and a default message.
	 * @since 5.1.4
	 */
	public WebClientResponseException(int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] body, @Nullable Charset charset,
			@Nullable HttpRequest request) {

		this(statusCode + " " + statusText, statusCode, statusText, headers, body, charset, request);
	}

	/**
	 * Constructor with a prepared message.
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset) {
		this(message, statusCode, statusText, headers, responseBody, charset, null);
	}

	/**
	 * Constructor with a prepared message.
	 * @since 5.1.4
	 */
	public WebClientResponseException(String message, int statusCode, String statusText,
			@Nullable HttpHeaders headers, @Nullable byte[] responseBody, @Nullable Charset charset,
			@Nullable HttpRequest request) {

		super(message);

		this.statusCode = statusCode;
		this.statusText = statusText;
		this.headers = (headers != null ? headers : HttpHeaders.EMPTY);
		this.responseBody = (responseBody != null ? responseBody : new byte[0]);
		this.responseCharset = (charset != null ? charset : StandardCharsets.ISO_8859_1);
		this.request = request;
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

	/**
	 * Return the corresponding request.
	 * @since 5.1.4
	 */
	@Nullable
	public HttpRequest getRequest() {
		return this.request;
	}

	/**
	 * Create {@code WebClientResponseException} or an HTTP status specific subclass.
	 * @since 5.1
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset) {

		return create(statusCode, statusText, headers, body, charset, null);
	}

	/**
	 * Create {@code WebClientResponseException} or an HTTP status specific subclass.
	 * @since 5.1.4
	 */
	public static WebClientResponseException create(
			int statusCode, String statusText, HttpHeaders headers, byte[] body,
			@Nullable Charset charset, @Nullable HttpRequest request) {

		HttpStatus httpStatus = HttpStatus.resolve(statusCode);
		if (httpStatus != null) {
			switch (httpStatus) {
				case BAD_REQUEST:
					return new WebClientResponseException.BadRequest(statusText, headers, body, charset, request);
				case UNAUTHORIZED:
					return new WebClientResponseException.Unauthorized(statusText, headers, body, charset, request);
				case FORBIDDEN:
					return new WebClientResponseException.Forbidden(statusText, headers, body, charset, request);
				case NOT_FOUND:
					return new WebClientResponseException.NotFound(statusText, headers, body, charset, request);
				case METHOD_NOT_ALLOWED:
					return new WebClientResponseException.MethodNotAllowed(statusText, headers, body, charset, request);
				case NOT_ACCEPTABLE:
					return new WebClientResponseException.NotAcceptable(statusText, headers, body, charset, request);
				case CONFLICT:
					return new WebClientResponseException.Conflict(statusText, headers, body, charset, request);
				case GONE:
					return new WebClientResponseException.Gone(statusText, headers, body, charset, request);
				case UNSUPPORTED_MEDIA_TYPE:
					return new WebClientResponseException.UnsupportedMediaType(statusText, headers, body, charset, request);
				case TOO_MANY_REQUESTS:
					return new WebClientResponseException.TooManyRequests(statusText, headers, body, charset, request);
				case UNPROCESSABLE_ENTITY:
					return new WebClientResponseException.UnprocessableEntity(statusText, headers, body, charset, request);
				case INTERNAL_SERVER_ERROR:
					return new WebClientResponseException.InternalServerError(statusText, headers, body, charset, request);
				case NOT_IMPLEMENTED:
					return new WebClientResponseException.NotImplemented(statusText, headers, body, charset, request);
				case BAD_GATEWAY:
					return new WebClientResponseException.BadGateway(statusText, headers, body, charset, request);
				case SERVICE_UNAVAILABLE:
					return new WebClientResponseException.ServiceUnavailable(statusText, headers, body, charset, request);
				case GATEWAY_TIMEOUT:
					return new WebClientResponseException.GatewayTimeout(statusText, headers, body, charset, request);
			}
		}
		return new WebClientResponseException(statusCode, statusText, headers, body, charset, request);
	}



	// Subclasses for specific, client-side, HTTP status codes

	/**
	 * {@link WebClientResponseException} for status HTTP 400 Bad Request.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadRequest extends WebClientResponseException {

		BadRequest(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.BAD_REQUEST.value(), statusText, headers, body, charset, request);
		}

	}

	/**
	 * {@link WebClientResponseException} for status HTTP 401 Unauthorized.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Unauthorized extends WebClientResponseException {

		Unauthorized(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.UNAUTHORIZED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 403 Forbidden.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Forbidden extends WebClientResponseException {

		Forbidden(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.FORBIDDEN.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 404 Not Found.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotFound extends WebClientResponseException {

		NotFound(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.NOT_FOUND.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 405 Method Not Allowed.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class MethodNotAllowed extends WebClientResponseException {

		MethodNotAllowed(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.METHOD_NOT_ALLOWED.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 406 Not Acceptable.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotAcceptable extends WebClientResponseException {

		NotAcceptable(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_ACCEPTABLE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 409 Conflict.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Conflict extends WebClientResponseException {

		Conflict(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.CONFLICT.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 410 Gone.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class Gone extends WebClientResponseException {

		Gone(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.GONE.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 415 Unsupported Media Type.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnsupportedMediaType extends WebClientResponseException {

		UnsupportedMediaType(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {

			super(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 422 Unprocessable Entity.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class UnprocessableEntity extends WebClientResponseException {

		UnprocessableEntity(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.UNPROCESSABLE_ENTITY.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 429 Too Many Requests.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class TooManyRequests extends WebClientResponseException {

		TooManyRequests(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.TOO_MANY_REQUESTS.value(), statusText, headers, body, charset,
					request);
		}
	}



	// Subclasses for specific, server-side, HTTP status codes

	/**
	 * {@link WebClientResponseException} for status HTTP 500 Internal Server Error.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class InternalServerError extends WebClientResponseException {

		InternalServerError(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.INTERNAL_SERVER_ERROR.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 501 Not Implemented.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class NotImplemented extends WebClientResponseException {

		NotImplemented(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.NOT_IMPLEMENTED.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP HTTP 502 Bad Gateway.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class BadGateway extends WebClientResponseException {

		BadGateway(String statusText, HttpHeaders headers, byte[] body, @Nullable Charset charset,
				@Nullable HttpRequest request) {
			super(HttpStatus.BAD_GATEWAY.value(), statusText, headers, body, charset, request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 503 Service Unavailable.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class ServiceUnavailable extends WebClientResponseException {

		ServiceUnavailable(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.SERVICE_UNAVAILABLE.value(), statusText, headers, body, charset,
					request);
		}
	}

	/**
	 * {@link WebClientResponseException} for status HTTP 504 Gateway Timeout.
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	public static class GatewayTimeout extends WebClientResponseException {

		GatewayTimeout(String statusText, HttpHeaders headers, byte[] body,
				@Nullable Charset charset, @Nullable HttpRequest request) {
			super(HttpStatus.GATEWAY_TIMEOUT.value(), statusText, headers, body, charset,
					request);
		}
	}

}
