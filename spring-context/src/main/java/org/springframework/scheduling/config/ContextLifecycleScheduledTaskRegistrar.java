/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scheduling.config;

import org.springframework.beans.factory.SmartInitializingSingleton;

/**
 * {@link ScheduledTaskRegistrar} subclass which redirects the actual scheduling
 * of tasks to the {@link #afterSingletonsInstantiated()} callback (as of 4.1.2).
 *
 * @author Juergen Hoeller
 * @since 3.2.1
 */
public class ContextLifecycleScheduledTaskRegistrar extends ScheduledTaskRegistrar implements SmartInitializingSingleton {

	@Override
	public void afterPropertiesSet() {
		// no-op
	}

	@Override
	public void afterSingletonsInstantiated() {
		scheduleTasks();
	}

}
