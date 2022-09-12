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

package org.springframework.web.observation;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.HttpStatus;

/**
 * Default {@link HttpRequestsObservationConvention}.
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultHttpRequestsObservationConvention implements HttpRequestsObservationConvention {

	private static final String DEFAULT_NAME = "http.server.requests";

	private static final KeyValue METHOD_UNKNOWN = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.METHOD, "UNKNOWN");

	private static final KeyValue STATUS_UNKNOWN = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.STATUS, "UNKNOWN");

	private static final KeyValue URI_UNKNOWN = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.URI, "UNKNOWN");

	private static final KeyValue URI_ROOT = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.URI, "root");

	private static final KeyValue URI_NOT_FOUND = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.URI, "NOT_FOUND");

	private static final KeyValue URI_REDIRECTION = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.URI, "REDIRECTION");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private static final KeyValue URI_EXPANDED_UNKNOWN = KeyValue.of(HttpRequestsObservation.HighCardinalityKeyNames.URI_EXPANDED, "UNKNOWN");

	private final String name;

	/**
	 * Create a convention with the default name {@code "http.server.requests"}.
	 */
	public DefaultHttpRequestsObservationConvention() {
		this(DEFAULT_NAME);
	}

	/**
	 * Create a convention with a custom name.
	 * @param name the observation name
	 */
	public DefaultHttpRequestsObservationConvention(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(HttpRequestsObservationContext context) {
		return KeyValues.of(method(context), uri(context), status(context), exception(context), outcome(context));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(HttpRequestsObservationContext context) {
		return KeyValues.of(uriExpanded(context));
	}

	protected KeyValue method(HttpRequestsObservationContext context) {
		return (context.getCarrier() != null) ? KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod()) : METHOD_UNKNOWN;
	}

	protected KeyValue status(HttpRequestsObservationContext context) {
		return (context.getResponse() != null) ? KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.STATUS, Integer.toString(context.getResponse().getStatus())) : STATUS_UNKNOWN;
	}

	protected KeyValue uri(HttpRequestsObservationContext context) {
		if (context.getCarrier() != null) {
			String pattern = context.getPathPattern();
			if (pattern != null) {
				if (pattern.isEmpty()) {
					return URI_ROOT;
				}
				return KeyValue.of("uri", pattern);
			}
			if (context.getResponse() != null) {
				HttpStatus status = HttpStatus.resolve(context.getResponse().getStatus());
				if (status != null) {
					if (status.is3xxRedirection()) {
						return URI_REDIRECTION;
					}
					if (status == HttpStatus.NOT_FOUND) {
						return URI_NOT_FOUND;
					}
				}
			}
		}
		return URI_UNKNOWN;
	}

	protected KeyValue exception(HttpRequestsObservationContext context) {
		return context.getError().map(throwable ->
						KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.EXCEPTION, throwable.getClass().getSimpleName()))
				.orElse(EXCEPTION_NONE);
	}

	protected KeyValue outcome(HttpRequestsObservationContext context) {
		if (context.getResponse() != null) {
			HttpStatus status = HttpStatus.resolve(context.getResponse().getStatus());
			if (status != null) {
				return KeyValue.of(HttpRequestsObservation.LowCardinalityKeyNames.OUTCOME, status.series().name());
			}
		}
		return OUTCOME_UNKNOWN;
	}

	protected KeyValue uriExpanded(HttpRequestsObservationContext context) {
		if (context.getCarrier() != null) {
			String uriExpanded = (context.getCarrier().getPathInfo() != null) ? context.getCarrier().getPathInfo() : "/";
			return KeyValue.of(HttpRequestsObservation.HighCardinalityKeyNames.URI_EXPANDED, uriExpanded);
		}
		return URI_EXPANDED_UNKNOWN;
	}

}
