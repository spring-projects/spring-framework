/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.config.conventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;

public class ExtendedServerRequestObservationConvention extends DefaultServerRequestObservationConvention {

	@Override
	public KeyValues getLowCardinalityKeyValues(ServerRequestObservationContext context) {
		// here, we just want to have an additional KeyValue to the observation, keeping the default values
		return super.getLowCardinalityKeyValues(context).and(custom(context));
	}

	private KeyValue custom(ServerRequestObservationContext context) {
		return KeyValue.of("custom.method", context.getCarrier().getMethod());
	}

}
