/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.config.conventions;


import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;

import org.springframework.http.server.observation.ServerRequestObservationContext;

public class ServerRequestObservationFilter implements ObservationFilter {

	@Override
	public Observation.Context map(Observation.Context context) {
		if (context instanceof ServerRequestObservationContext serverContext) {
			context.setName("custom.observation.name");
			context.addLowCardinalityKeyValue(KeyValue.of("project", "spring"));
			String customAttribute = (String) serverContext.getCarrier().getAttribute("customAttribute");
			context.addLowCardinalityKeyValue(KeyValue.of("custom.attribute", customAttribute));
		}
		return context;
	}
}
