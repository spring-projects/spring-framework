/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets started.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextRestartedEvent
 * @see ContextStoppedEvent
 */
@SuppressWarnings("serial")
public class ContextStartedEvent extends ApplicationContextEvent {

	/**
	 * Create a new {@code ContextStartedEvent}.
	 * @param source the {@code ApplicationContext} that has been started
	 * (must not be {@code null})
	 */
	public ContextStartedEvent(ApplicationContext source) {
		super(source);
	}

}
