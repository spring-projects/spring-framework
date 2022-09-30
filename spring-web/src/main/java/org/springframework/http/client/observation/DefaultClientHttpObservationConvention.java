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

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.observation.HttpOutcome;
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

	private static final KeyValue STATUS_IO_ERROR = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.STATUS, "IO_ERROR");

	private static final KeyValue STATUS_CLIENT_ERROR = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.STATUS, "CLIENT_ERROR");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue URI_EXPANDED_NONE = KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.URI_EXPANDED, "none");

	private static final KeyValue CLIENT_NAME_NONE = KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.CLIENT_NAME, "none");

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
	public String getContextualName(ClientHttpObservationContext context) {
		return "http " + context.getCarrier().getMethod().name().toLowerCase();
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
		ClientHttpResponse response = context.getResponse();
		if (response == null) {
			return STATUS_CLIENT_ERROR;
		}
		try {
			return KeyValue.of(ClientHttpObservation.LowCardinalityKeyNames.STATUS, String.valueOf(response.getStatusCode().value()));
		}
		catch (IOException ex) {
			return STATUS_IO_ERROR;
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
		if (context.getResponse() != null) {
			try {
				HttpOutcome httpOutcome = HttpOutcome.forStatus(context.getResponse().getStatusCode());
				return httpOutcome.asKeyValue();
			}
			catch (IOException ex) {
				// Continue
			}
		}
		return HttpOutcome.UNKNOWN.asKeyValue();
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
		if (context.getCarrier() != null && context.getCarrier().getURI().getHost() != null) {
			return KeyValue.of(ClientHttpObservation.HighCardinalityKeyNames.CLIENT_NAME, context.getCarrier().getURI().getHost());
		}
		return CLIENT_NAME_NONE;
	}

}
