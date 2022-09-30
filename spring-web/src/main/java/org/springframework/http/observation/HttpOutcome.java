/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.observation;

import io.micrometer.common.KeyValue;

import org.springframework.http.HttpStatusCode;

/**
 * The outcome of an HTTP request.
 * <p>Used as the {@code "outcome"} {@link io.micrometer.common.KeyValue}
 * for HTTP {@link io.micrometer.observation.Observation observations}.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @since 6.0
 */
public enum HttpOutcome {

	/**
	 * Outcome of the request was informational.
	 */
	INFORMATIONAL,

	/**
	 * Outcome of the request was success.
	 */
	SUCCESS,

	/**
	 * Outcome of the request was redirection.
	 */
	REDIRECTION,

	/**
	 * Outcome of the request was client error.
	 */
	CLIENT_ERROR,

	/**
	 * Outcome of the request was server error.
	 */
	SERVER_ERROR,

	/**
	 * Outcome of the request was unknown.
	 */
	UNKNOWN;

	private final KeyValue keyValue;

	HttpOutcome() {
		this.keyValue = KeyValue.of("outcome", name());
	}

	/**
	 * Returns the {@code Outcome} as a {@link KeyValue} named {@code outcome}.
	 * @return the {@code outcome} {@code KeyValue}
	 */
	public KeyValue asKeyValue() {
		return this.keyValue;
	}

	/**
	 * Return the {@code HttpOutcome} for the given HTTP {@code status} code.
	 * @param status the HTTP status code
	 * @return the matching HttpOutcome
	 */
	public static HttpOutcome forStatus(HttpStatusCode status) {
		if (status.is1xxInformational()) {
			return INFORMATIONAL;
		}
		else if (status.is2xxSuccessful()) {
			return SUCCESS;
		}
		else if (status.is3xxRedirection()) {
			return REDIRECTION;
		}
		else if (status.is4xxClientError()) {
			return CLIENT_ERROR;
		}
		else if (status.is5xxServerError()) {
			return SERVER_ERROR;
		}
		return UNKNOWN;
	}
}
