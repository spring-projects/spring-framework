/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.support;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;

/**
 * Interface for an {@link ObservationConvention} for
 * {@link ScheduledTaskObservationDocumentation#TASKS_SCHEDULED_EXECUTION scheduled task executions}.
 *
 * @author Brian Clozel
 * @since 6.1
 */
public interface ScheduledTaskObservationConvention extends ObservationConvention<ScheduledTaskObservationContext> {

	@Override
	default boolean supportsContext(Observation.Context context) {
		return context instanceof ScheduledTaskObservationContext;
	}

}
