/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.http;

import org.jspecify.annotations.Nullable;

/**
 * Enumeration of HTTP status codes.
 *
 * <p>The HTTP status code series can be retrieved via {@link #series()}.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 3.0
 * @see HttpStatus.Series
 * @see <a href="https://www.iana.org/assignments/http-status-codes">HTTP Status Code Registry</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes - Wikipedia</a>
 */
public enum HttpStatus implements HttpStatusCode {

	// 1xx Informational

	/**
	 * {@code 100 Continue}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">HTTP/1.1: Semantics and Content, section 6.2.1</a>
	 */
	CONTINUE(100, Series.INFORMATIONAL, "Continue"),
	/**
	 * {@code 101 Switching Protocols}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">HTTP/1.1: Semantics and Content, section 6.2.2</a>
	 */
	SWITCHING_PROTOCOLS(101, Series.INFORMATIONAL, "Switching Protocols"),
	/**
	 * {@code 102 Processing}.
	 * @see <a href="https://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
	 */
	PROCESSING(102, Series.INFORMATIONAL, "Processing"),
	/**
	 * {@code 103 Early Hints}.
	 * @see <a href="https://tools.ietf.org/html/rfc8297">An HTTP Status Code for Indicating Hints</a>
	 * @since 6.0.5
	 */
	EARLY_HINTS(103, Series.INFORMATIONAL, "Early Hints"),

	// 2xx Success

	/**
	 * {@code 200 OK}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1: Semantics and Content, section 6.3.1</a>
	 */
	OK(200, Series.SUCCESSFUL, "OK"),
	/**
	 * {@code 201 Created}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1: Semantics and Content, section 6.3.2</a>
	 */
	CREATED(201, Series.SUCCESSFUL, "Created"),
	/**
	 * {@code 202 Accepted}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1: Semantics and Content, section 6.3.3</a>
	 */
	ACCEPTED(202, Series.SUCCESSFUL, "Accepted"),
	/**
	 * {@code 203 Non-Authoritative Information}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1: Semantics and Content, section 6.3.4</a>
	 */
	NON_AUTHORITATIVE_INFORMATION(203, Series.SUCCESSFUL, "Non-Authoritative Information"),
	/**
	 * {@code 204 No Content}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1: Semantics and Content, section 6.3.5</a>
	 */
	NO_CONTENT(204, Series.SUCCESSFUL, "No Content"),
	/**
	 * {@code 205 Reset Content}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.6">HTTP/1.1: Semantics and Content, section 6.3.6</a>
	 */
	RESET_CONTENT(205, Series.SUCCESSFUL, "Reset Content"),
	/**
	 * {@code 206 Partial Content}.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.1">HTTP/1.1: Range Requests, section 4.1</a>
	 */
	PARTIAL_CONTENT(206, Series.SUCCESSFUL, "Partial Content"),
	/**
	 * {@code 207 Multi-Status}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
	 */
	MULTI_STATUS(207, Series.SUCCESSFUL, "Multi-Status"),
	/**
	 * {@code 208 Already Reported}.
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.1">WebDAV Binding Extensions</a>
	 */
	ALREADY_REPORTED(208, Series.SUCCESSFUL, "Already Reported"),
	/**
	 * {@code 226 IM Used}.
	 * @see <a href="https://tools.ietf.org/html/rfc3229#section-10.4.1">Delta encoding in HTTP</a>
	 */
	IM_USED(226, Series.SUCCESSFUL, "IM Used"),

	// 3xx Redirection

	/**
	 * {@code 300 Multiple Choices}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1: Semantics and Content, section 6.4.1</a>
	 */
	MULTIPLE_CHOICES(300, Series.REDIRECTION, "Multiple Choices"),
	/**
	 * {@code 301 Moved Permanently}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1: Semantics and Content, section 6.4.2</a>
	 */
	MOVED_PERMANENTLY(301, Series.REDIRECTION, "Moved Permanently"),
	/**
	 * {@code 302 Found}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1: Semantics and Content, section 6.4.3</a>
	 */
	FOUND(302, Series.REDIRECTION, "Found"),
	/**
	 * {@code 303 See Other}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1: Semantics and Content, section 6.4.4</a>
	 */
	SEE_OTHER(303, Series.REDIRECTION, "See Other"),
	/**
	 * {@code 304 Not Modified}.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.1">HTTP/1.1: Conditional Requests, section 4.1</a>
	 */
	NOT_MODIFIED(304, Series.REDIRECTION, "Not Modified"),
	/**
	 * {@code 307 Temporary Redirect}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1: Semantics and Content, section 6.4.7</a>
	 */
	TEMPORARY_REDIRECT(307, Series.REDIRECTION, "Temporary Redirect"),
	/**
	 * {@code 308 Permanent Redirect}.
	 * @see <a href="https://tools.ietf.org/html/rfc7238">RFC 7238</a>
	 */
	PERMANENT_REDIRECT(308, Series.REDIRECTION, "Permanent Redirect"),

	// --- 4xx Client Error ---

	/**
	 * {@code 400 Bad Request}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1: Semantics and Content, section 6.5.1</a>
	 */
	BAD_REQUEST(400, Series.CLIENT_ERROR, "Bad Request"),
	/**
	 * {@code 401 Unauthorized}.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: Authentication, section 3.1</a>
	 */
	UNAUTHORIZED(401, Series.CLIENT_ERROR, "Unauthorized"),
	/**
	 * {@code 402 Payment Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">HTTP/1.1: Semantics and Content, section 6.5.2</a>
	 */
	PAYMENT_REQUIRED(402, Series.CLIENT_ERROR, "Payment Required"),
	/**
	 * {@code 403 Forbidden}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: Semantics and Content, section 6.5.3</a>
	 */
	FORBIDDEN(403, Series.CLIENT_ERROR, "Forbidden"),
	/**
	 * {@code 404 Not Found}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1: Semantics and Content, section 6.5.4</a>
	 */
	NOT_FOUND(404, Series.CLIENT_ERROR, "Not Found"),
	/**
	 * {@code 405 Method Not Allowed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1: Semantics and Content, section 6.5.5</a>
	 */
	METHOD_NOT_ALLOWED(405, Series.CLIENT_ERROR, "Method Not Allowed"),
	/**
	 * {@code 406 Not Acceptable}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1: Semantics and Content, section 6.5.6</a>
	 */
	NOT_ACCEPTABLE(406, Series.CLIENT_ERROR, "Not Acceptable"),
	/**
	 * {@code 407 Proxy Authentication Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.2">HTTP/1.1: Authentication, section 3.2</a>
	 */
	PROXY_AUTHENTICATION_REQUIRED(407, Series.CLIENT_ERROR, "Proxy Authentication Required"),
	/**
	 * {@code 408 Request Timeout}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1: Semantics and Content, section 6.5.7</a>
	 */
	REQUEST_TIMEOUT(408, Series.CLIENT_ERROR, "Request Timeout"),
	/**
	 * {@code 409 Conflict}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1: Semantics and Content, section 6.5.8</a>
	 */
	CONFLICT(409, Series.CLIENT_ERROR, "Conflict"),
	/**
	 * {@code 410 Gone}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">
	 *     HTTP/1.1: Semantics and Content, section 6.5.9</a>
	 */
	GONE(410, Series.CLIENT_ERROR, "Gone"),
	/**
	 * {@code 411 Length Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">
	 *     HTTP/1.1: Semantics and Content, section 6.5.10</a>
	 */
	LENGTH_REQUIRED(411, Series.CLIENT_ERROR, "Length Required"),
	/**
	 * {@code 412 Precondition failed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.2">
	 *     HTTP/1.1: Conditional Requests, section 4.2</a>
	 */
	PRECONDITION_FAILED(412, Series.CLIENT_ERROR, "Precondition Failed"),
	/**
	 * {@code 413 Payload Too Large}.
	 * @since 4.1
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">
	 *     HTTP/1.1: Semantics and Content, section 6.5.11</a>
	 */
	PAYLOAD_TOO_LARGE(413, Series.CLIENT_ERROR, "Payload Too Large"),
	/**
	 * {@code 414 URI Too Long}.
	 * @since 4.1
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">
	 *     HTTP/1.1: Semantics and Content, section 6.5.12</a>
	 */
	URI_TOO_LONG(414, Series.CLIENT_ERROR, "URI Too Long"),
	/**
	 * {@code 415 Unsupported Media Type}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">
	 *     HTTP/1.1: Semantics and Content, section 6.5.13</a>
	 */
	UNSUPPORTED_MEDIA_TYPE(415, Series.CLIENT_ERROR, "Unsupported Media Type"),
	/**
	 * {@code 416 Requested Range Not Satisfiable}.
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.4">HTTP/1.1: Range Requests, section 4.4</a>
	 */
	REQUESTED_RANGE_NOT_SATISFIABLE(416, Series.CLIENT_ERROR, "Requested range not satisfiable"),
	/**
	 * {@code 417 Expectation Failed}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.14">
	 *     HTTP/1.1: Semantics and Content, section 6.5.14</a>
	 */
	EXPECTATION_FAILED(417, Series.CLIENT_ERROR, "Expectation Failed"),
	/**
	 * {@code 418 I'm a teapot}.
	 * @see <a href="https://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
	 */
	I_AM_A_TEAPOT(418, Series.CLIENT_ERROR, "I'm a teapot"),
	/**
	 * {@code 422 Unprocessable Entity}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
	 */
	UNPROCESSABLE_ENTITY(422, Series.CLIENT_ERROR, "Unprocessable Entity"),
	/**
	 * {@code 423 Locked}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
	 */
	LOCKED(423, Series.CLIENT_ERROR, "Locked"),
	/**
	 * {@code 424 Failed Dependency}.
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
	 */
	FAILED_DEPENDENCY(424, Series.CLIENT_ERROR, "Failed Dependency"),
	/**
	 * {@code 425 Too Early}.
	 * @since 5.2
	 * @see <a href="https://tools.ietf.org/html/rfc8470">RFC 8470</a>
	 */
	TOO_EARLY(425, Series.CLIENT_ERROR, "Too Early"),
	/**
	 * {@code 426 Upgrade Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc2817#section-6">Upgrading to TLS Within HTTP/1.1</a>
	 */
	UPGRADE_REQUIRED(426, Series.CLIENT_ERROR, "Upgrade Required"),
	/**
	 * {@code 428 Precondition Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-3">Additional HTTP Status Codes</a>
	 */
	PRECONDITION_REQUIRED(428, Series.CLIENT_ERROR, "Precondition Required"),
	/**
	 * {@code 429 Too Many Requests}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-4">Additional HTTP Status Codes</a>
	 */
	TOO_MANY_REQUESTS(429, Series.CLIENT_ERROR, "Too Many Requests"),
	/**
	 * {@code 431 Request Header Fields Too Large}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-5">Additional HTTP Status Codes</a>
	 */
	REQUEST_HEADER_FIELDS_TOO_LARGE(431, Series.CLIENT_ERROR, "Request Header Fields Too Large"),
	/**
	 * {@code 451 Unavailable For Legal Reasons}.
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-legally-restricted-status-04">
	 * An HTTP Status Code to Report Legal Obstacles</a>
	 * @since 4.3
	 */
	UNAVAILABLE_FOR_LEGAL_REASONS(451, Series.CLIENT_ERROR, "Unavailable For Legal Reasons"),

	// --- 5xx Server Error ---

	/**
	 * {@code 500 Internal Server Error}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1: Semantics and Content, section 6.6.1</a>
	 */
	INTERNAL_SERVER_ERROR(500, Series.SERVER_ERROR, "Internal Server Error"),
	/**
	 * {@code 501 Not Implemented}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1: Semantics and Content, section 6.6.2</a>
	 */
	NOT_IMPLEMENTED(501, Series.SERVER_ERROR, "Not Implemented"),
	/**
	 * {@code 502 Bad Gateway}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1: Semantics and Content, section 6.6.3</a>
	 */
	BAD_GATEWAY(502, Series.SERVER_ERROR, "Bad Gateway"),
	/**
	 * {@code 503 Service Unavailable}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1: Semantics and Content, section 6.6.4</a>
	 */
	SERVICE_UNAVAILABLE(503, Series.SERVER_ERROR, "Service Unavailable"),
	/**
	 * {@code 504 Gateway Timeout}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1: Semantics and Content, section 6.6.5</a>
	 */
	GATEWAY_TIMEOUT(504, Series.SERVER_ERROR, "Gateway Timeout"),
	/**
	 * {@code 505 HTTP Version Not Supported}.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">HTTP/1.1: Semantics and Content, section 6.6.6</a>
	 */
	HTTP_VERSION_NOT_SUPPORTED(505, Series.SERVER_ERROR, "HTTP Version not supported"),
	/**
	 * {@code 506 Variant Also Negotiates}
	 * @see <a href="https://tools.ietf.org/html/rfc2295#section-8.1">Transparent Content Negotiation</a>
	 */
	VARIANT_ALSO_NEGOTIATES(506, Series.SERVER_ERROR, "Variant Also Negotiates"),
	/**
	 * {@code 507 Insufficient Storage}
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
	 */
	INSUFFICIENT_STORAGE(507, Series.SERVER_ERROR, "Insufficient Storage"),
	/**
	 * {@code 508 Loop Detected}
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.2">WebDAV Binding Extensions</a>
 	 */
	LOOP_DETECTED(508, Series.SERVER_ERROR, "Loop Detected"),
	/**
	 * {@code 509 Bandwidth Limit Exceeded}
 	 */
	BANDWIDTH_LIMIT_EXCEEDED(509, Series.SERVER_ERROR, "Bandwidth Limit Exceeded"),
	/**
	 * {@code 510 Not Extended}
	 * @see <a href="https://tools.ietf.org/html/rfc2774#section-7">HTTP Extension Framework</a>
	 */
	NOT_EXTENDED(510, Series.SERVER_ERROR, "Not Extended"),
	/**
	 * {@code 511 Network Authentication Required}.
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-6">Additional HTTP Status Codes</a>
	 */
	NETWORK_AUTHENTICATION_REQUIRED(511, Series.SERVER_ERROR, "Network Authentication Required");


	private static final HttpStatus[] VALUES;

	static {
		VALUES = values();
	}


	private final int value;

	private final Series series;

	private final String reasonPhrase;

	HttpStatus(int value, Series series, String reasonPhrase) {
		this.value = value;
		this.series = series;
		this.reasonPhrase = reasonPhrase;
	}


	@Override
	public int value() {
		return this.value;
	}

	/**
	 * Return the HTTP status series of this status code.
	 * @see HttpStatus.Series
	 */
	public Series series() {
		return this.series;
	}

	/**
	 * Return the reason phrase of this status code.
	 */
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}

	@Override
	public boolean is1xxInformational() {
		return (series() == Series.INFORMATIONAL);
	}

	@Override
	public boolean is2xxSuccessful() {
		return (series() == Series.SUCCESSFUL);
	}

	@Override
	public boolean is3xxRedirection() {
		return (series() == Series.REDIRECTION);
	}

	@Override
	public boolean is4xxClientError() {
		return (series() == Series.CLIENT_ERROR);
	}

	@Override
	public boolean is5xxServerError() {
		return (series() == Series.SERVER_ERROR);
	}

	@Override
	public boolean isError() {
		return (is4xxClientError() || is5xxServerError());
	}

	/**
	 * Return a string representation of this status code.
	 */
	@Override
	public String toString() {
		return this.value + " " + name();
	}


	/**
	 * Return the {@code HttpStatus} enum constant with the specified numeric value.
	 * @param statusCode the numeric value of the enum to be returned
	 * @return the enum constant with the specified numeric value
	 * @throws IllegalArgumentException if this enum has no constant for the specified numeric value
	 */
	public static HttpStatus valueOf(int statusCode) {
		HttpStatus status = resolve(statusCode);
		if (status == null) {
			throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
		}
		return status;
	}

	/**
	 * Resolve the given status code to an {@code HttpStatus}, if possible.
	 * @param statusCode the HTTP status code (potentially non-standard)
	 * @return the corresponding {@code HttpStatus}, or {@code null} if not found
	 * @since 5.0
	 */
	public static @Nullable HttpStatus resolve(int statusCode) {
		// Use cached VALUES instead of values() to prevent array allocation.
		for (HttpStatus status : VALUES) {
			if (status.value == statusCode) {
				return status;
			}
		}
		return null;
	}


	/**
	 * Enumeration of HTTP status series.
	 * <p>Retrievable via {@link HttpStatus#series()}.
	 */
	public enum Series {

		INFORMATIONAL(1),
		SUCCESSFUL(2),
		REDIRECTION(3),
		CLIENT_ERROR(4),
		SERVER_ERROR(5);

		private final int value;

		Series(int value) {
			this.value = value;
		}

		/**
		 * Return the integer value of this status series. Ranges from 1 to 5.
		 */
		public int value() {
			return this.value;
		}

		/**
		 * Return the {@code Series} enum constant for the supplied status code.
		 * @param statusCode the HTTP status code (potentially non-standard)
		 * @return the {@code Series} enum constant for the supplied status code
		 * @throws IllegalArgumentException if this enum has no corresponding constant
		 */
		public static Series valueOf(int statusCode) {
			Series series = resolve(statusCode);
			if (series == null) {
				throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
			}
			return series;
		}

		/**
		 * Resolve the given status code to an {@code HttpStatus.Series}, if possible.
		 * @param statusCode the HTTP status code (potentially non-standard)
		 * @return the corresponding {@code Series}, or {@code null} if not found
		 * @since 5.1.3
		 */
		public static @Nullable Series resolve(int statusCode) {
			int seriesCode = statusCode / 100;
			for (Series series : values()) {
				if (series.value == seriesCode) {
					return series;
				}
			}
			return null;
		}
	}

}
