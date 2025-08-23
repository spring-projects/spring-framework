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

package org.springframework.http.server.reactive.observation;

import java.util.Locale;
import java.util.Set;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.observation.ServerHttpObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.http.server.reactive.observation.ServerHttpObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.util.StringUtils;

/**
 * Default {@link ServerRequestObservationConvention}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultServerRequestObservationConvention implements ServerRequestObservationConvention {

	private static final String DEFAULT_NAME = "http.server.requests";

	private static final KeyValue METHOD_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.METHOD, "UNKNOWN");

	private static final KeyValue STATUS_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.STATUS, "UNKNOWN");

	private static final KeyValue HTTP_OUTCOME_SUCCESS = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "SUCCESS");

	private static final KeyValue HTTP_OUTCOME_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private static final KeyValue URI_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.URI, "UNKNOWN");

	private static final KeyValue URI_ROOT = KeyValue.of(LowCardinalityKeyNames.URI, "root");

	private static final KeyValue URI_NOT_FOUND = KeyValue.of(LowCardinalityKeyNames.URI, "NOT_FOUND");

	private static final KeyValue URI_REDIRECTION = KeyValue.of(LowCardinalityKeyNames.URI, "REDIRECTION");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(LowCardinalityKeyNames.EXCEPTION, KeyValue.NONE_VALUE);

	private static final KeyValue HTTP_URL_UNKNOWN = KeyValue.of(HighCardinalityKeyNames.HTTP_URL, "UNKNOWN");

	private static final Set<HttpMethod> HTTP_METHODS = Set.of(HttpMethod.values());


	private final String name;


	/**
	 * Create a convention with the default name {@code "http.server.requests"}.
	 */
	public DefaultServerRequestObservationConvention() {
		this(DEFAULT_NAME);
	}

	/**
	 * Create a convention with a custom name.
	 * @param name the observation name
	 */
	public DefaultServerRequestObservationConvention(String name) {
		this.name = name;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public @Nullable String getContextualName(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			String httpMethod = context.getCarrier().getMethod().name().toLowerCase(Locale.ROOT);
			if (context.getPathPattern() != null) {
				return "http " + httpMethod + " " + context.getPathPattern();
			}
			return "http " + httpMethod;
		}
		return null;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(exception(context), method(context), outcome(context), status(context), uri(context));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(httpUrl(context));
	}

	protected KeyValue method(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			HttpMethod method = context.getCarrier().getMethod();
			if (HTTP_METHODS.contains(method)) {
				return KeyValue.of(LowCardinalityKeyNames.METHOD, method.name());
			}
		}
		return METHOD_UNKNOWN;
	}

	protected KeyValue status(ServerRequestObservationContext context) {
		if (context.isConnectionAborted() && (context.getResponse() == null || !context.getResponse().isCommitted())) {
			return STATUS_UNKNOWN;
		}
		return (context.getResponse() != null && context.getResponse().getStatusCode() != null) ?
				KeyValue.of(LowCardinalityKeyNames.STATUS, Integer.toString(context.getResponse().getStatusCode().value())) :
				STATUS_UNKNOWN;
	}

	protected KeyValue uri(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			String pattern = context.getPathPattern();
			if (pattern != null) {
				if (pattern.isEmpty()) {
					return URI_ROOT;
				}
				return KeyValue.of(LowCardinalityKeyNames.URI, pattern);
			}
			if (context.getResponse() != null && context.getResponse().getStatusCode() != null) {
				HttpStatus status = HttpStatus.resolve(context.getResponse().getStatusCode().value());
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

	protected KeyValue exception(ServerRequestObservationContext context) {
		Throwable error = context.getError();
		if (error != null) {
			String simpleName = error.getClass().getSimpleName();
			return KeyValue.of(LowCardinalityKeyNames.EXCEPTION, StringUtils.hasText(simpleName) ?
					simpleName : error.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	protected KeyValue outcome(ServerRequestObservationContext context) {
		if (context.isConnectionAborted()) {
			return HTTP_OUTCOME_UNKNOWN;
		}
		if (context.getResponse() != null && context.getResponse().getStatusCode() != null) {
			return HttpOutcome.forStatus(context.getResponse().getStatusCode());
		}
		return HTTP_OUTCOME_UNKNOWN;
	}

	protected KeyValue httpUrl(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(HighCardinalityKeyNames.HTTP_URL, context.getCarrier().getPath().toString());
		}
		return HTTP_URL_UNKNOWN;
	}


	static class HttpOutcome {

		static KeyValue forStatus(HttpStatusCode statusCode) {
			if (statusCode.is2xxSuccessful()) {
				return HTTP_OUTCOME_SUCCESS;
			}
			else if (statusCode instanceof HttpStatus status) {
				return KeyValue.of(LowCardinalityKeyNames.OUTCOME, status.series().name());
			}
			else {
				return HTTP_OUTCOME_UNKNOWN;
			}
		}
	}

}
