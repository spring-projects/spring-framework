package org.springframework.context;

/**
 * Listener that maintains a global count of events.
 *
 * @author Rod Johnson
 * @since January 21, 2001
 */
public class TestListener implements ApplicationListener<ApplicationEvent> {

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
