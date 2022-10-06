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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.observation.HttpOutcome;
import org.springframework.util.StringUtils;

/**
 * Default {@link HttpRequestsObservationConvention}.
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultHttpRequestsObservationConvention implements HttpRequestsObservationConvention {

	private static final String DEFAULT_NAME = "http.server.requests";

	private static final KeyValue METHOD_UNKNOWN = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.METHOD, "UNKNOWN");

	private static final KeyValue STATUS_UNKNOWN = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.STATUS, "UNKNOWN");

	private static final KeyValue URI_UNKNOWN = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.URI, "UNKNOWN");

	private static final KeyValue URI_ROOT = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.URI, "root");

	private static final KeyValue URI_NOT_FOUND = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.URI, "NOT_FOUND");

	private static final KeyValue URI_REDIRECTION = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.URI, "REDIRECTION");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue HTTP_URL_UNKNOWN = KeyValue.of(HttpRequestsObservationDocumentation.HighCardinalityKeyNames.HTTP_URL, "UNKNOWN");

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
	public String getContextualName(HttpRequestsObservationContext context) {
		return "http " + context.getCarrier().getMethod().toLowerCase();
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
		return (context.getCarrier() != null) ? KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod()) : METHOD_UNKNOWN;
	}

	protected KeyValue status(HttpRequestsObservationContext context) {
		return (context.getResponse() != null) ? KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.STATUS, Integer.toString(context.getResponse().getStatus())) : STATUS_UNKNOWN;
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
		Throwable error = context.getError();
		if (error != null) {
			String simpleName = error.getClass().getSimpleName();
			return KeyValue.of(HttpRequestsObservationDocumentation.LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : error.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	protected KeyValue outcome(HttpRequestsObservationContext context) {
		if (context.getResponse() != null) {
			HttpStatusCode statusCode = HttpStatusCode.valueOf(context.getResponse().getStatus());
			HttpOutcome httpOutcome = HttpOutcome.forStatus(statusCode);
			return httpOutcome.asKeyValue();
		}
		return HttpOutcome.UNKNOWN.asKeyValue();
	}

	protected KeyValue uriExpanded(HttpRequestsObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(HttpRequestsObservationDocumentation.HighCardinalityKeyNames.HTTP_URL, context.getCarrier().getRequestURI());
		}
		return HTTP_URL_UNKNOWN;
	}

}
