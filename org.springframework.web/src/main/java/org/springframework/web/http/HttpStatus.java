/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.http;

/**
 * Java 5 enumeration of HTTP status codes. <p/> The HTTP status code series can be retrieved via {@link #series()}.
 *
 * @author Arjen Poutsma
 * @see HttpStatus.Series
 */
public enum HttpStatus {

	// 1xx Informational

	CONTINUE(100),
	SWITCHING_PROTOCOLS(101),

	// 2xx Success

	OK(200),
	CREATED(201),
	ACCEPTED(202),
	NON_AUTHORITATIVE_INFORMATION(203),
	NO_CONTENT(204),
	RESET_CONTENT(205),
	PARTIAL_CONTENT(206),

	// 3xx Redirection

	MULTIPLE_CHOICES(300),
	MOVED_PERMANENTLY(301),
	MOVED_TEMPORARILY(302),
	SEE_OTHER(303),
	NOT_MODIFIED(304),
	USE_PROXY(305),
	TEMPORARY_REDIRECT(307),

	// --- 4xx Client Error ---

	BAD_REQUEST(400),
	UNAUTHORIZED(401),
	PAYMENT_REQUIRED(402),
	FORBIDDEN(403),
	NOT_FOUND(404),
	METHOD_NOT_ALLOWED(405),
	NOT_ACCEPTABLE(406),
	PROXY_AUTHENTICATION_REQUIRED(407),
	REQUEST_TIMEOUT(408),
	CONFLICT(409),
	GONE(410),
	LENGTH_REQUIRED(411),
	PRECONDITION_FAILED(412),
	REQUEST_TOO_LONG(413),
	REQUEST_URI_TOO_LONG(414),
	UNSUPPORTED_MEDIA_TYPE(415),
	REQUESTED_RANGE_NOT_SATISFIABLE(416),
	EXPECTATION_FAILED(417),

	// --- 5xx Server Error ---

	INTERNAL_SERVER_ERROR(500),
	NOT_IMPLEMENTED(501),
	BAD_GATEWAY(502),
	SERVICE_UNAVAILABLE(503),
	GATEWAY_TIMEOUT(504),
	HTTP_VERSION_NOT_SUPPORTED(505);

	/**
	 * Java 5 enumeration of HTTP status series. <p/> Retrievable via {@link HttpStatus#series()}.
	 */
	public enum Series {

		INFORMATIONAL(1),
		SUCCESSFUL(2),
		REDIRECTION(3),
		CLIENT_ERROR(4),
		SERVER_ERROR(5);

		private final int value;

		private Series(int value) {
			this.value = value;
		}

		/**
		 * Returns the integer value of this status series. Ranges from 1 to 5.
		 *
		 * @return the integer value
		 */
		public int value() {
			return value;
		}

		private static Series valueOf(HttpStatus status) {
			int seriesCode = status.value() / 100;
			for (Series series : values()) {
				if (series.value == seriesCode) {
					return series;
				}
			}
			throw new IllegalArgumentException("No matching constant for [" + status + "]");
		}

	}

	private final int value;

	private HttpStatus(int value) {
		this.value = value;
	}

	/**
	 * Returns the integer value of this status code.
	 *
	 * @return the integer value
	 */
	public int value() {
		return value;
	}

	/**
	 * Returns the HTTP status series of this status code.
	 *
	 * @return the series
	 * @see HttpStatus.Series
	 */
	public Series series() {
		return Series.valueOf(this);
	}

	/**
	 * Returns the enum constant of this type with the specified numeric value.
	 *
	 * @param statusCode the numeric value of the enum to be returned
	 * @return the enum constant with the specified numeric value
	 * @throws IllegalArgumentException if this enum has no constant for the specified numeric value
	 */
	public static HttpStatus valueOf(int statusCode) {
		for (HttpStatus status : values()) {
			if (status.value == statusCode) {
				return status;
			}
		}
		throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
	}

	/**
	 * Returns a string representation of this status code.
	 *
	 * @return a string representation
	 */
	@Override
	public String toString() {
		return Integer.toString(value);
	}

}
