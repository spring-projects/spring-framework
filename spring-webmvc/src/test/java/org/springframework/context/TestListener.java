package org.springframework.context;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Listener that maintains a global count of events.
 *
 * @author Rod Johnson
 * @since January 21, 2001
 */
public class TestListener implements ApplicationListener {

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