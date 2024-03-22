/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http.client.observation;

import java.io.IOException;
import java.util.regex.Pattern;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.observation.ClientHttpObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.http.client.observation.ClientHttpObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation for a {@link ClientRequestObservationConvention},
 * extracting information from the {@link ClientRequestObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultClientRequestObservationConvention implements ClientRequestObservationConvention {

	private static final String DEFAULT_NAME = "http.client.requests";

	private static final Pattern PATTERN_BEFORE_PATH = Pattern.compile("^https?://[^/]+/");

	private static final KeyValue URI_NONE = KeyValue.of(LowCardinalityKeyNames.URI, KeyValue.NONE_VALUE);

	private static final KeyValue METHOD_NONE = KeyValue.of(LowCardinalityKeyNames.METHOD, KeyValue.NONE_VALUE);

	private static final KeyValue STATUS_IO_ERROR = KeyValue.of(LowCardinalityKeyNames.STATUS, "IO_ERROR");

	private static final KeyValue STATUS_CLIENT_ERROR = KeyValue.of(LowCardinalityKeyNames.STATUS, "CLIENT_ERROR");

	private static final KeyValue HTTP_OUTCOME_SUCCESS = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "SUCCESS");

	private static final KeyValue HTTP_OUTCOME_UNKNOWN = KeyValue.of(LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private static final KeyValue CLIENT_NAME_NONE = KeyValue.of(LowCardinalityKeyNames.CLIENT_NAME, KeyValue.NONE_VALUE);

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(LowCardinalityKeyNames.EXCEPTION, KeyValue.NONE_VALUE);

	private static final KeyValue HTTP_URL_NONE = KeyValue.of(HighCardinalityKeyNames.HTTP_URL, KeyValue.NONE_VALUE);


	private final String name;

	/**
	 * Create a convention with the default name {@code "http.client.requests"}.
	 */
	public DefaultClientRequestObservationConvention() {
		this(DEFAULT_NAME);
	}

	/**
	 * Create a convention with a custom name.
	 * @param name the observation name
	 */
	public DefaultClientRequestObservationConvention(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@Nullable
	public String getContextualName(ClientRequestObservationContext context) {
		ClientHttpRequest request = context.getCarrier();
		return (request != null ? "http " + request.getMethod().name().toLowerCase() : null);
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ClientRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(clientName(context), exception(context), method(context), outcome(context), status(context), uri(context));
	}

	protected KeyValue uri(ClientRequestObservationContext context) {
		if (context.getUriTemplate() != null) {
			return KeyValue.of(LowCardinalityKeyNames.URI, extractPath(context.getUriTemplate()));
		}
		return URI_NONE;
	}

	private static String extractPath(String uriTemplate) {
		String path = PATTERN_BEFORE_PATH.matcher(uriTemplate).replaceFirst("");
		return (path.startsWith("/") ? path : "/" + path);
	}

	protected KeyValue method(ClientRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(ClientRequestObservationContext context) {
		ClientHttpResponse response = context.getResponse();
		if (response == null) {
			return STATUS_CLIENT_ERROR;
		}
		try {
			return KeyValue.of(LowCardinalityKeyNames.STATUS, String.valueOf(response.getStatusCode().value()));
		}
		catch (IOException ex) {
			return STATUS_IO_ERROR;
		}
	}

	protected KeyValue clientName(ClientRequestObservationContext context) {
		if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
			return KeyValue.of(LowCardinalityKeyNames.CLIENT_NAME, context.getCarrier().getURI().getHost());
		}
		return CLIENT_NAME_NONE;
	}

	protected KeyValue exception(ClientRequestObservationContext context) {
		Throwable error = context.getError();
		if (error != null) {
			String simpleName = error.getClass().getSimpleName();
			return KeyValue.of(LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : error.getClass().getName());
		}
		return EXCEPTION_NONE;
	}

	protected KeyValue outcome(ClientRequestObservationContext context) {
		if (context.getResponse() != null) {
			try {
				return HttpOutcome.forStatus(context.getResponse().getStatusCode());
			}
			catch (IOException ex) {
				// Continue
			}
		}
		return HTTP_OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(requestUri(context));
	}

	protected KeyValue requestUri(ClientRequestObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(HighCardinalityKeyNames.HTTP_URL, context.getCarrier().getURI().toASCIIString());
		}
		return HTTP_URL_NONE;
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
