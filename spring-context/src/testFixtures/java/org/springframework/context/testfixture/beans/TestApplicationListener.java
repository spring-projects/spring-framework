/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.testfixture.beans;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Listener that maintains a global count of events.
 *
 * @author Rod Johnson
 * @since January 21, 2001
 */
public class TestApplicationListener implements ApplicationListener<ApplicationEvent> {

	private int eventCount;

	public int getEventCount() {
		return eventCount;
	}

	public void zeroCounter() {
		eventCount = 0;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent e) {
		++eventCount;
	}

}
