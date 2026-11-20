/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test utility to collect and assert events.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
@Component
public class EventCollector {

	private final MultiValueMap<String, Object> content = new LinkedMultiValueMap<>();


	/**
	 * Register an event for the specified listener.
	 */
	public void addEvent(Identifiable listener, Object event) {
		this.content.add(listener.getId(), event);
	}

	/**
	 * Return the events that the specified listener has received. The list of events
	 * is ordered according to the order in which they were received.
	 */
	public List<Object> getEvents(Identifiable listener) {
		return this.content.get(listener.getId());
	}

	/**
	 * Assert that the listener identified by the specified id has not received any event.
	 */
	public void assertNoEventReceived(String listenerId) {
		List<Object> events = this.content.getOrDefault(listenerId, Collections.emptyList());
		assertThat(events).as("Expected no events but got " + events).isEmpty();
	}

	/**
	 * Assert that the specified listener has not received any event.
	 */
	public void assertNoEventReceived(Identifiable listener) {
		assertNoEventReceived(listener.getId());
	}

	/**
	 * Assert that the listener identified by the specified id has received the
	 * specified events, in that specific order.
	 */
	public void assertEvent(String listenerId, Object... events) {
		List<Object> actual = this.content.getOrDefault(listenerId, Collections.emptyList());
		assertThat(actual).as("Wrong number of events").hasSameSizeAs(events);
		for (int i = 0; i < events.length; i++) {
			assertThat(actual.get(i)).as("Wrong event at index " + i).isEqualTo(events[i]);
		}
	}

	/**
	 * Assert that the specified listener has received the specified events, in
	 * that specific order.
	 */
	public void assertEvent(Identifiable listener, Object... events) {
		assertEvent(listener.getId(), events);
	}

	/**
	 * Assert the number of events received by this instance. Checks that
	 * unexpected events have not been received. If an event is handled by
	 * several listeners, each instance will be registered.
	 */
	public void assertTotalEventsCount(int number) {
		int actual = 0;
		for (Map.Entry<String, List<Object>> entry : this.content.entrySet()) {
			actual += entry.getValue().size();
		}
		assertThat(actual).as("Wrong number of total events (" + this.content.size() +
				") registered listener(s)").isEqualTo(number);
	}

	/**
	 * Clear the collected events, allowing for reuse of the collector.
	 */
	public void clear() {
		this.content.clear();
	}

}
