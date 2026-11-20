/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import java.util.function.Consumer;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * Extended variant of the standard {@link ApplicationListener} interface,
 * exposing further metadata such as the supported event and source type.
 *
 * <p>As of Spring Framework 4.2, this interface supersedes the Class-based
 * {@link SmartApplicationListener} with full handling of generic event types.
 * As of 5.3.5, it formally extends {@link SmartApplicationListener}, adapting
 * {@link #supportsEventType(Class)} to {@link #supportsEventType(ResolvableType)}
 * with a default method.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 * @see SmartApplicationListener
 * @see GenericApplicationListenerAdapter
 */
public interface GenericApplicationListener extends SmartApplicationListener {

	/**
	 * Overrides {@link SmartApplicationListener#supportsEventType(Class)} with
	 * delegation to {@link #supportsEventType(ResolvableType)}.
	 */
	@Override
	default boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
		return supportsEventType(ResolvableType.forClass(eventType));
	}

	/**
	 * Determine whether this listener actually supports the given event type.
	 * @param eventType the event type (never {@code null})
	 */
	boolean supportsEventType(ResolvableType eventType);


	/**
	 * Create a new {@code ApplicationListener} for the given event type.
	 * @param eventType the event to listen to
	 * @param consumer the consumer to invoke when a matching event is fired
	 * @param <E> the specific {@code ApplicationEvent} subclass to listen to
	 * @return a corresponding {@code ApplicationListener} instance
	 * @since 6.1.3
	 */
	static <E extends ApplicationEvent> GenericApplicationListener forEventType(Class<E> eventType, Consumer<E> consumer) {
		return new GenericApplicationListenerDelegate<>(eventType, consumer);
	}

}
