/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import java.util.Set;

/**
 * Common interface for exposing locally scheduled tasks.
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 * @see ScheduledTaskRegistrar
 * @see org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
 */
public interface ScheduledTaskHolder {

	/**
	 * Return an overview of the tasks that have been scheduled by this instance.
	 */
	Set<ScheduledTask> getScheduledTasks();

}
