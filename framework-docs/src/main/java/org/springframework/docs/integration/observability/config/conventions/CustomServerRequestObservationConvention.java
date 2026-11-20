/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.config.conventions;

import java.util.Locale;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.server.observation.ServerHttpObservationDocumentation;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;

public class CustomServerRequestObservationConvention implements ServerRequestObservationConvention {

	@Override
	public String getName() {
		// will be used as the metric name
		return "http.server.requests";
	}

	@Override
	public String getContextualName(ServerRequestObservationContext context) {
		// will be used for the trace name
		return "http " + context.getCarrier().getMethod().toLowerCase(Locale.ROOT);
	}

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		return KeyValues.of(method(context), status(context), exception(context));
	}


	@Override
	public KeyValues getHighCardinalityKeyValues(ServerRequestObservationContext context) {
		return KeyValues.of(httpUrl(context));
	}

	private KeyValue method(ServerRequestObservationContext context) {
		// You should reuse as much as possible the corresponding ObservationDocumentation for key names
		return KeyValue.of(ServerHttpObservationDocumentation.LowCardinalityKeyNames.METHOD, context.getCarrier().getMethod());
	}

	// @fold:on // status(), exception(), httpUrl()...
	private KeyValue status(ServerRequestObservationContext context) {
		return KeyValue.of(ServerHttpObservationDocumentation.LowCardinalityKeyNames.STATUS, String.valueOf(context.getResponse().getStatus()));
	}

	private KeyValue exception(ServerRequestObservationContext context) {
		String exception = (context.getError() != null ? context.getError().getClass().getSimpleName() : KeyValue.NONE_VALUE);
		return KeyValue.of(ServerHttpObservationDocumentation.LowCardinalityKeyNames.EXCEPTION, exception);
	}

	private KeyValue httpUrl(ServerRequestObservationContext context) {
		return KeyValue.of(ServerHttpObservationDocumentation.HighCardinalityKeyNames.HTTP_URL, context.getCarrier().getRequestURI());
	}
	// @fold:off

}
