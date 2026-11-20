/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets stopped.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 * @see ContextStartedEvent
 * @see ContextRestartedEvent
 */
@SuppressWarnings("serial")
public class ContextStoppedEvent extends ApplicationContextEvent {

	/**
	 * Create a new {@code ContextStoppedEvent}.
	 * @param source the {@code ApplicationContext} that has been stopped
	 * (must not be {@code null})
	 */
	public ContextStoppedEvent(ApplicationContext source) {
		super(source);
	}

}
