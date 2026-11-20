/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets paused.
 *
 * <p>Note that {@code ContextPausedEvent} is a specialization of
 * {@link ContextStoppedEvent}.
 *
 * @author Juergen Hoeller
 * @since 7.0
 * @see ConfigurableApplicationContext#pause()
 * @see ContextRestartedEvent
 * @see ContextStoppedEvent
 */
@SuppressWarnings("serial")
public class ContextPausedEvent extends ContextStoppedEvent {

	/**
	 * Create a new {@code ContextPausedEvent}.
	 * @param source the {@code ApplicationContext} that has been paused
	 * (must not be {@code null})
	 */
	public ContextPausedEvent(ApplicationContext source) {
		super(source);
	}

}
