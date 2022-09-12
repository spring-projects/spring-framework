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

package org.springframework.http.client.observation;

import java.io.IOException;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation for a {@link ClientHttpObservationConvention},
 * extracting information from the {@link ClientHttpObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultClientHttpObservationConvention implements ClientHttpObservationConvention {

	private static final String DEFAULT_NAME = "http.client.requests";

	private static final KeyValue URI_NONE = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.URI, "none");

	private static final KeyValue METHOD_NONE = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.METHOD, "none");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private static final KeyValue URI_EXPANDED_NONE = KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.URI_EXPANDED, "none");

	private final String name;

	/**
	 * Create a convention with the default name {@code "http.client.requests"}.
	 */
	public DefaultClientHttpObservationConvention() {
		this(DEFAULT_NAME);
	}

	/**
	 * Create a convention with a custom name.
	 * @param name the observation name
	 */
	public DefaultClientHttpObservationConvention(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ClientHttpObservationContext context) {
		return KeyValues.of(uri(context), method(context), status(context), exception(context), outcome(context));
	}

	protected KeyValue uri(ClientHttpObservationContext context) {
		if (context.getUriTemplate() != null) {
			return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.URI, context.getUriTemplate());
		}
		return URI_NONE;
	}

	protected KeyValue method(ClientHttpObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(ClientHttpObservationContext context) {
		return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.STATUS, getStatusMessage(context.getResponse()));
	}

	private String getStatusMessage(@Nullable ClientHttpResponse response) {
		try {
			if (response == null) {
				return "CLIENT_ERROR";
			}
			return String.valueOf(response.getStatusCode().value());
		}
		catch (IOException ex) {
			return "IO_ERROR";
		}
	}

	protected KeyValue exception(ClientHttpObservationContext context) {
		return context.getError().map(exception -> {
			String simpleName = exception.getClass().getSimpleName();
			return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}).orElse(EXCEPTION_NONE);
	}

	protected static KeyValue outcome(ClientHttpObservationContext context) {
		try {
			if (context.getResponse() != null) {
				HttpStatus status = HttpStatus.resolve(context.getResponse().getStatusCode().value());
				if (status != null) {
					return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.OUTCOME, status.series().name());
				}
			}
		}
		catch (IOException ex) {
			// Continue
		}
		return OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ClientHttpObservationContext context) {
		return KeyValues.of(requestUri(context), clientName(context));
	}

	protected KeyValue requestUri(ClientHttpObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.URI_EXPANDED, context.getCarrier().getURI().toASCIIString());
		}
		return URI_EXPANDED_NONE;
	}

	protected KeyValue clientName(ClientHttpObservationContext context) {
		String host = "none";
		if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
			host = context.getCarrier().getURI().getHost();
		}
		return KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.CLIENT_NAME, host);
	}

}
