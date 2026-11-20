/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;

/**
 * Default implementation of {@link ApplicationEvents}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.3
 */
class DefaultApplicationEvents implements ApplicationEvents {

	private final List<ApplicationEvent> events = new CopyOnWriteArrayList<>();


	void addEvent(ApplicationEvent event) {
		this.events.add(event);
	}

	@Override
	public Stream<ApplicationEvent> stream() {
		return this.events.stream();
	}

	@Override
	public <T> Stream<T> stream(Class<T> type) {
		return this.events.stream()
				.map(this::unwrapPayloadEvent)
				.filter(type::isInstance)
				.map(type::cast);
	}

	@Override
	public void clear() {
		this.events.clear();
	}

	private Object unwrapPayloadEvent(Object source) {
		return ((source instanceof PayloadApplicationEvent<?> payloadApplicationEvent) ?
				payloadApplicationEvent.getPayload() : source);
	}

}
