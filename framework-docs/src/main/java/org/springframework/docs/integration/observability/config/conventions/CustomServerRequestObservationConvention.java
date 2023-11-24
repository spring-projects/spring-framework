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

package org.springframework.docs.integration.observability.config.conventions;

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
		return "http " + context.getCarrier().getMethod().toLowerCase();
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
