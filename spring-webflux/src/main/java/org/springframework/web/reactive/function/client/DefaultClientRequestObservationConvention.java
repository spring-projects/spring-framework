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

package org.springframework.web.reactive.function.client;

import java.io.IOException;
import java.util.regex.Pattern;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientHttpObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.web.reactive.function.client.ClientHttpObservationDocumentation.LowCardinalityKeyNames;

/**
 * Default implementation for a {@link ClientRequestObservationConvention},
 * extracting information from the {@link ClientRequestObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultClientRequestObservationConvention implements ClientRequestObservationConvention {

	private static final String DEFAULT_NAME = "http.client.requests";

	private static final String ROOT_PATH = "/";

	private static final Pattern PATTERN_BEFORE_PATH = Pattern.compile("^https?://[^/]+/");

	private static final KeyValue URI_NONE = KeyValue.of(LowCardinalityKeyNames.URI, KeyValue.NONE_VALUE);

	private static final KeyValue URI_ROOT = KeyValue.of(LowCardinalityKeyNames.URI, ROOT_PATH);

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
	public String getContextualName(ClientRequestObservationContext context) {
		return "http " + context.getRequest().method().name().toLowerCase();
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
		ClientRequest request = context.getRequest();
		if (request != null && ROOT_PATH.equals(request.url().getPath())) {
			return URI_ROOT;
		}
		return URI_NONE;
	}

	private static String extractPath(String uriTemplate) {
		String path = PATTERN_BEFORE_PATH.matcher(uriTemplate).replaceFirst("");
		return (path.startsWith("/") ? path : "/" + path);
	}

	protected KeyValue method(ClientRequestObservationContext context) {
		if (context.getRequest() != null) {
			return KeyValue.of(LowCardinalityKeyNames.METHOD, context.getRequest().method().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(ClientRequestObservationContext context) {
		if (context.isAborted()) {
			return STATUS_CLIENT_ERROR;
		}
		ClientResponse response = context.getResponse();
		if (response != null) {
			return KeyValue.of(LowCardinalityKeyNames.STATUS, String.valueOf(response.statusCode().value()));
		}
		if (context.getError() != null && context.getError() instanceof IOException) {
			return STATUS_IO_ERROR;
		}
		return STATUS_CLIENT_ERROR;
	}

	protected KeyValue clientName(ClientRequestObservationContext context) {
		if (context.getRequest() != null && context.getRequest().url().getHost() != null) {
			return KeyValue.of(LowCardinalityKeyNames.CLIENT_NAME, context.getRequest().url().getHost());
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
		if (context.isAborted()) {
			return HTTP_OUTCOME_UNKNOWN;
		}
		if (context.getResponse() != null) {
			return HttpOutcome.forStatus(context.getResponse().statusCode());
		}
		return HTTP_OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ClientRequestObservationContext context) {
		// Make sure that KeyValues entries are already sorted by name for better performance
		return KeyValues.of(httpUrl(context));
	}

	protected KeyValue httpUrl(ClientRequestObservationContext context) {
		if (context.getRequest() != null) {
			return KeyValue.of(HighCardinalityKeyNames.HTTP_URL, context.getRequest().url().toASCIIString());
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
