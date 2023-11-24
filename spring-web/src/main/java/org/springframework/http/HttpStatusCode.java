/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * Represents an HTTP response status code. Implemented by {@link HttpStatus},
 * but defined as an interface to allow for values not in that enumeration.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see <a href="https://www.iana.org/assignments/http-status-codes">HTTP Status Code Registry</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes - Wikipedia</a>
 */
public sealed interface HttpStatusCode extends Serializable permits DefaultHttpStatusCode, HttpStatus {

	/**
	 * Return the integer value of this status code.
	 */
	int value();

	/**
	 * Whether this status code is in the Informational class ({@code 1xx}).
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.1">RFC 2616</a>
	 */
	boolean is1xxInformational();

	/**
	 * Whether this status code is in the Successful class ({@code 2xx}).
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.2">RFC 2616</a>
	 */
	boolean is2xxSuccessful();

	/**
	 * Whether this status code is in the Redirection class ({@code 3xx}).
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
	 */
	boolean is3xxRedirection();

	/**
	 * Whether this status code is in the Client Error class ({@code 4xx}).
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
	 */
	boolean is4xxClientError();

	/**
	 * Whether this status code is in the Server Error class ({@code 5xx}).
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.5">RFC 2616</a>
	 */
	boolean is5xxServerError();

	/**
	 * Whether this status code is in the Client or Server Error class
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.4">RFC 2616</a>
	 * @see <a href="https://datatracker.ietf.org/doc/html/rfc2616#section-10.3">RFC 2616</a>
	 * ({@code 4xx} or {@code 5xx}).
	 * @see #is4xxClientError()
	 * @see #is5xxServerError()
	 */
	boolean isError();

	/**
	 * Whether this {@code HttpStatusCode} shares the same integer {@link #value() value} as the other status code.
	 * <p>Useful for comparisons that take deprecated aliases into account or compare arbitrary implementations
	 * of {@code HttpStatusCode} (e.g. in place of {@link HttpStatus#equals(Object) HttpStatus enum equality}).
	 * @param other the other {@code HttpStatusCode} to compare
	 * @return true if the two {@code HttpStatusCode} objects share the same integer {@code value()}, false otherwise
	 * @since 6.0.5
	 */
	default boolean isSameCodeAs(HttpStatusCode other) {
		return value() == other.value();
	}

	/**
	 * Return an {@code HttpStatusCode} object for the given integer value.
	 * @param code the status code as integer
	 * @return the corresponding {@code HttpStatusCode}
	 * @throws IllegalArgumentException if {@code code} is not a three-digit
	 * positive number
	 */
	static HttpStatusCode valueOf(int code) {
		Assert.isTrue(code >= 100 && code <= 999,
				() -> "Status code '" + code + "' should be a three-digit positive integer");
		HttpStatus status = HttpStatus.resolve(code);
		if (status != null) {
			return status;
		}
		else {
			return new DefaultHttpStatusCode(code);
		}
	}

}
