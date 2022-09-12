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

package org.springframework.web.reactive.function.client;

import java.io.IOException;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.observation.ObservationConvention;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * Default implementation for a {@code WebClient} {@link ObservationConvention},
 * extracting information from the {@link ClientObservationContext}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class DefaultClientObservationConvention implements ClientObservationConvention {

	private static final String DEFAULT_NAME = "http.client.requests";

	private static final KeyValue URI_NONE = KeyValue.of(ClientObservation.LowCardinalityKeyNames.URI, "none");

	private static final KeyValue METHOD_NONE = KeyValue.of(ClientObservation.LowCardinalityKeyNames.METHOD, "none");

	private static final KeyValue EXCEPTION_NONE = KeyValue.of(ClientObservation.LowCardinalityKeyNames.EXCEPTION, "none");

	private static final KeyValue OUTCOME_UNKNOWN = KeyValue.of(ClientObservation.LowCardinalityKeyNames.OUTCOME, "UNKNOWN");

	private final String name;


	/**
	 * Create a convention with the default name {@code "http.client.requests"}.
	 */
	public DefaultClientObservationConvention() {
		this(DEFAULT_NAME);
	}

	/**
	 * Create a convention with a custom name.
	 * @param name the observation name
	 */
	public DefaultClientObservationConvention(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ClientObservationContext context) {
		return KeyValues.of(uri(context), method(context), status(context), exception(context), outcome(context));
	}

	protected KeyValue uri(ClientObservationContext context) {
		if (context.getUriTemplate() != null) {
			return KeyValue.of(ClientObservation.LowCardinalityKeyNames.URI, context.getUriTemplate());
		}
		return URI_NONE;
	}

	protected KeyValue method(ClientObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(ClientObservation.LowCardinalityKeyNames.METHOD, context.getCarrier().method().name());
		}
		else {
			return METHOD_NONE;
		}
	}

	protected KeyValue status(ClientObservationContext context) {
		return KeyValue.of(ClientObservation.LowCardinalityKeyNames.STATUS, getStatusMessage(context));
	}

	private String getStatusMessage(ClientObservationContext context) {
		if (context.getResponse() != null) {
			return String.valueOf(context.getResponse().statusCode().value());
		}
		if (context.getError().isPresent()) {
			return (context.getError().get() instanceof IOException) ? "IO_ERROR" : "CLIENT_ERROR";
		}
		return "CLIENT_ERROR";
	}

	protected KeyValue exception(ClientObservationContext context) {
		return context.getError().map(exception -> {
			String simpleName = exception.getClass().getSimpleName();
			return KeyValue.of(ClientObservation.LowCardinalityKeyNames.EXCEPTION,
					StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}).orElse(EXCEPTION_NONE);
	}

	protected static KeyValue outcome(ClientObservationContext context) {
		if (context.isAborted()) {
			return OUTCOME_UNKNOWN;
		}
		else if (context.getResponse() != null) {
			HttpStatus status = HttpStatus.resolve(context.getResponse().statusCode().value());
			if (status != null) {
				return KeyValue.of(ClientObservation.LowCardinalityKeyNames.OUTCOME, status.series().name());
			}
		}
		return OUTCOME_UNKNOWN;
	}

	@Override
	public KeyValues getHighCardinalityKeyValues(ClientObservationContext context) {
		return KeyValues.of(uriExpanded(context), clientName(context));
	}

	protected KeyValue uriExpanded(ClientObservationContext context) {
		if (context.getCarrier() != null) {
			return KeyValue.of(ClientObservation.HighCardinalityKeyNames.URI_EXPANDED, context.getCarrier().url().toASCIIString());
		}
		return KeyValue.of(ClientObservation.HighCardinalityKeyNames.URI_EXPANDED, "none");
	}

	protected KeyValue clientName(ClientObservationContext context) {
		String host = "none";
		if (context.getCarrier() != null && context.getCarrier().url().getHost() != null) {
			host = context.getCarrier().url().getHost();
		}
		return KeyValue.of(ClientObservation.HighCardinalityKeyNames.CLIENT_NAME, host);
	}

}
