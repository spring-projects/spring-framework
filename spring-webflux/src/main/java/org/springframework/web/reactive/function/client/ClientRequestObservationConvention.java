/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} related to
 * {@link ClientHttpObservationDocumentation#HTTP_REACTIVE_CLIENT_EXCHANGES HTTP client exchange observations}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public interface ClientRequestObservationConvention extends ObservationConvention<ClientRequestObservationContext> {

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ClientRequestObservationContext;
	}

}
