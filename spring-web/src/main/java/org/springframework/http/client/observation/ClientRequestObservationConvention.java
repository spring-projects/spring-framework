/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for
 * {@link ClientHttpObservationDocumentation#HTTP_CLIENT_EXCHANGES client HTTP exchanges}.
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
