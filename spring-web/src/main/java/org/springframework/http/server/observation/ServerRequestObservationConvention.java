/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for {@link ServerHttpObservationDocumentation#HTTP_SERVLET_SERVER_REQUESTS Servlet HTTP requests}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public interface ServerRequestObservationConvention extends ObservationConvention<ServerRequestObservationContext> {

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ServerRequestObservationContext;
	}

}
