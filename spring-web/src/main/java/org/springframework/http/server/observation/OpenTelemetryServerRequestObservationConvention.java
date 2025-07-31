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

package org.springframework.http.server.observation;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.observation.OpenTelemetryServerHttpObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.http.server.observation.OpenTelemetryServerHttpObservationDocumentation.LowCardinalityKeyNames;

/**
 * A {@link ServerRequestObservationConvention} based on the stable OpenTelemetry semantic conventions.
 *
 * @author Brian Clozel
 * @author Tommy Ludwig
 * @since 7.0
 * @see OpenTelemetryServerHttpObservationDocumentation
 */
public class OpenTelemetryServerRequestObservationConvention implements ServerRequestObservationConvention {

	private static final String NAME = "http.server.request.duration";

	private static final KeyValue METHOD_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.METHOD, "_OTHER");

	private static final KeyValue SCHEME_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.SCHEME, "UNKNOWN");

	private static final KeyValue STATUS_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.STATUS, "UNKNOWN");

	private static final KeyValue HTTP_OUTCOME_SUCCESS = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "SUCCESS");

	private static final KeyValue HTTP_OUTCOME_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private static final KeyValue ROUTE_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.ROUTE, "UNKNOWN");

	private static final KeyValue ROUTE_ROOT = KeyValue.of(LowCardinalityKeyNames.ROUTE, "root");

	private static final KeyValue ROUTE_NOT_FOUND = KeyValue.of(LowCardinalityKeyNames.ROUTE, "NOT_FOUND");

	private static final KeyValue ROUTE_REDIRECTION = KeyValue.of(LowCardinalityKeyNames.ROUTE, "REDIRECTION");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(LowCardinalityKeyNames.EXCEPTION, KeyValue.NONE_VALUE);

	private static final KeyValue HTTP_URL_UNKNOWN = KeyValue.of(HighCardinalityKeyNames.URL_PATH, "UNKNOWN");

	private static final KeyValue ORIGINAL_METHOD_UNKNOWN = KeyValue.of(HighCardinalityKeyNames.METHOD_ORIGINAL, "UNKNOWN");

	private static final Set<String> HTTP_METHODS = Stream.of(HttpMethod.values()).map(HttpMethod::name).collect(Collectors.toUnmodifiableSet());


	/**
	 * Create a convention.
	 */
	public OpenTelemetryServerRequestObservationConvention() {
	}


	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * HTTP span names SHOULD be {@code {method} {target}} if there is a (low-cardinality) {@code target}
	 * available. If there is no (low-cardinality) {@code {target}} available, HTTP span names
	 * SHOULD be {@code {method}}.
	 * <p>
	 * The {@code {method}} MUST be {@code {http.request.method}} if the method represents the original
	 * method known to the instrumentation. In other cases (when Customize Toolbar… is
	 * set to {@code _OTHER}), {@code {method}} MUST be HTTP.
	 * <p>
	 * The {@code target} SHOULD be the {@code {http.route}}.
	 * @param context context
	 * @return contextual name
	 * @see <a href="https://github.com/open-telemetry/semantic-conventions/blob/v1.36.0/docs/http/http-spans.md#name">OpenTelemetry Semantic Convention HTTP Span Name (v1.36.0)</a>
	 */
	@Override
	public String getContextualName(ServerRequestObservationContext context) {
		if (context.getCarrier() == null) {
			return "HTTP";
		}
		String maybeMethod = getMethodValue(context);
		String method = maybeMethod == null ? "HTTP" : maybeMethod;
		String target = context.getPathPattern();
		if (target != null) {
			return method + " " + target;
		}
		return method;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(exception(context), method(context), status(context), pathTemplate(context), outcome(context), scheme(context));
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(methodOriginal(context), httpUrl(context));
	}

	protected KeyValue method(ServerRequestObservationContext context) {
		String method = getMethodValue(context);
		if (method != null) {
			return KeyValue.of(LowCardinalityKeyNames.METHOD, method);
		}
		return METHOD_UNKNOWN;
	}

	protected @Nullable String getMethodValue(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			String httpMethod = context.getCarrier().getMethod();
			if (HTTP_METHODS.contains(httpMethod)) {
				return httpMethod;
			}
		}
		return null;
	}

	protected KeyValue scheme(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(LowCardinalityKeyNames.SCHEME, context.getCarrier().getScheme());
		}
		return SCHEME_UNKNOWN;
	}

	protected KeyValue status(ServerRequestObservationContext context) {
		return (context.getResponse() != null) ?
				KeyValue.of(LowCardinalityKeyNames.STATUS, Integer.toString(context.getResponse().getStatus())) :
				STATUS_UNKNOWN;
	}

	protected KeyValue pathTemplate(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			String pattern = context.getPathPattern();
			if (pattern != null) {
				if (pattern.isEmpty()) {
					return ROUTE_ROOT;
				}
				return KeyValue.of(LowCardinalityKeyNames.ROUTE, pattern);
			}
			if (context.getResponse() != null) {
				HttpStatus status = HttpStatus.resolve(context.getResponse().getStatus());
				if (status != null) {
					if (status.is3xxRedirection()) {
						return ROUTE_REDIRECTION;
					}
					if (status == HttpStatus.NOT_FOUND) {
						return ROUTE_NOT_FOUND;
					}
				}
			}
		}
		return ROUTE_UNKNOWN;
	}

	protected KeyValue exception(ServerRequestObservationContext context) {
		Throwable error = context.getError();
		if (error != null) {
			return KeyValue.of(LowCardinalityKeyNames.EXCEPTION, error.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	protected KeyValue outcome(ServerRequestObservationContext context) {
		try {
			if (context.getResponse() != null) {
				HttpStatusCode statusCode = HttpStatusCode.valueOf(context.getResponse().getStatus());
				return HttpOutcome.forStatus(statusCode);
			}
		}
		catch (IllegalArgumentException ex) {
			return HTTP_OUTCOME_UNKNOWN;
		}
		return HTTP_OUTCOME_UNKNOWN;
	}

	protected KeyValue httpUrl(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(HighCardinalityKeyNames.URL_PATH, context.getCarrier().getRequestURI());
		}
		return HTTP_URL_UNKNOWN;
	}

	protected KeyValue methodOriginal(ServerRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(HighCardinalityKeyNames.METHOD_ORIGINAL, context.getCarrier().getMethod());
		}
		return ORIGINAL_METHOD_UNKNOWN;
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
