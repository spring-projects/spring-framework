/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * {@link ApplicationListener} that listens to all events and adds them to the
 * current {@link ApplicationEvents} instance if registered for the current thread.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 */
class ApplicationEventsApplicationListener implements ApplicationListener<ApplicationEvent> {

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		DefaultApplicationEvents applicationEvents =
				(DefaultApplicationEvents) ApplicationEventsHolder.getApplicationEvents();
		if (applicationEvents != null) {
			applicationEvents.addEvent(event);
		}
	}

}
