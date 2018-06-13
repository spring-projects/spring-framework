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

package org.springframework.web.reactive.function.client;

import java.util.function.Predicate;

import org.springframework.util.Assert;

/**
 * Provides various methods for creating status code predicates.
 *
 * @author Denys Ivano
 * @since 5.0.7
 */
public abstract class StatusCodePredicates {

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 1xx Informational series.
	 * @return a predicate that matches on 1xx status codes
	 */
	public static Predicate<Integer> is1xxInformational() {
		return between(100, 199);
	}

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 2xx Successful series.
	 * @return a predicate that matches on 2xx status codes
	 */
	public static Predicate<Integer> is2xxSuccessful() {
		return between(200, 299);
	}

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 3xx Redirection series.
	 * @return a predicate that matches on 3xx status codes
	 */
	public static Predicate<Integer> is3xxRedirection() {
		return between(300, 399);
	}

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 4xx Client Error series.
	 * @return a predicate that matches on 4xx status codes
	 */
	public static Predicate<Integer> is4xxClientError() {
		return between(400, 499);
	}

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 5xx Server Error series.
	 * @return a predicate that matches on 5xx status codes
	 */
	public static Predicate<Integer> is5xxServerError() {
		return between(500, 599);
	}

	/**
	 * Return a status code predicate that tests whether the status code is in the
	 * HTTP 4xx Client Error or 5xx Server Error series.
	 * @return a predicate that matches on 4xx and 5xx status codes
	 */
	public static Predicate<Integer> isError() {
		return between(400, 599);
	}

	/**
	 * Return a status code predicate that tests whether the status code is between
	 * the specified values.
	 * @param from value from (inclusive)
	 * @param to value to (inclusive)
	 * @return the status code predicate
	 * @throws IllegalArgumentException if 'from' value is greater than 'to'
	 */
	public static Predicate<Integer> between(int from, int to) {
		Assert.isTrue(from <= to, "'from' value must be <= than 'to'");
		return status -> status >= from && status <= to;
	}

	/**
	 * Return a status code predicate that tests whether the status code is equal
	 * to the specified status.
	 * @param statusCode the status code to test against
	 * @return the status code predicate
	 */
	public static Predicate<Integer> is(int statusCode) {
		return status -> status == statusCode;
	}

	/**
	 * Return a status code predicate that tests whether the status code is not equal
	 * to the specified status.
	 * @param statusCode the status code to test against
	 * @return the status code predicate
	 */
	public static Predicate<Integer> not(int statusCode) {
		return status -> status != statusCode;
	}

	/**
	 * Return a status code predicate that tests whether the status code is among
	 * the specified statuses.
	 * @param statusCodes the status codes to test against
	 * @return the status code predicate
	 */
	public static Predicate<Integer> anyOf(int ... statusCodes) {
		Assert.notNull(statusCodes, "Status codes must not be null");
		return status -> {
			for (int s : statusCodes) {
				if (s == status) {
					return true;
				}
			}
			return false;
		};
	}

	/**
	 * Return a status code predicate that tests whether the status code is not among
	 * the specified statuses.
	 * @param statusCodes the status codes to test against
	 * @return the status code predicate
	 */
	public static Predicate<Integer> noneOf(int ... statusCodes) {
		return anyOf(statusCodes).negate();
	}

}
