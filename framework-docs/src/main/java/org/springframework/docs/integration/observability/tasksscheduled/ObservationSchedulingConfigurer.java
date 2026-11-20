/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.observability.tasksscheduled;


import io.micrometer.observation.ObservationRegistry;

import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

public class ObservationSchedulingConfigurer implements SchedulingConfigurer {

	private final ObservationRegistry observationRegistry;

	public ObservationSchedulingConfigurer(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setObservationRegistry(this.observationRegistry);
	}

}
