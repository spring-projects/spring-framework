/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets restarted.
 *
 * <p>Note that {@code ContextRestartedEvent} is a specialization of
 * {@link ContextStartedEvent}.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see ConfigurableApplicationContext#restart()
 * @see ContextPausedEvent
 * @see ContextStartedEvent
 */
@SuppressWarnings("serial")
public class ContextRestartedEvent extends ContextStartedEvent {

	/**
	 * Create a new {@code ContextRestartedEvent}.
	 * @param source the {@code ApplicationContext} that has been restarted
	 * (must not be {@code null})
	 */
	public ContextRestartedEvent(ApplicationContext source) {
		super(source);
	}

}
