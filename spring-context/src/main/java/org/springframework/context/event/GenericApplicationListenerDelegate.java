/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import java.util.function.Consumer;

import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;

/**
 * A {@link GenericApplicationListener} implementation that supports a single event type.
 *
 * @author Stephane Nicoll
 * @since 6.1.3
 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
 */
class GenericApplicationListenerDelegate<E extends ApplicationEvent> implements GenericApplicationListener {

	private final Class<E> supportedEventType;

	private final Consumer<E> consumer;


	GenericApplicationListenerDelegate(Class<E> supportedEventType, Consumer<E> consumer) {
		this.supportedEventType = supportedEventType;
		this.consumer = consumer;
	}


	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		this.consumer.accept(this.supportedEventType.cast(event));
	}

	@Override
	public boolean supportsEventType(ResolvableType eventType) {
		return this.supportedEventType.isAssignableFrom(eventType.toClass());
	}

}
