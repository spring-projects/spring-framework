/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets initialized or refreshed.
 *
 * @author Juergen Hoeller
 * @since 04.03.2003
 * @see ContextClosedEvent
 */
@SuppressWarnings("serial")
public class ContextRefreshedEvent extends ApplicationContextEvent {

	/**
	 * Create a new {@code ContextRefreshedEvent}.
	 * @param source the {@code ApplicationContext} that has been initialized
	 * or refreshed (must not be {@code null})
	 */
	public ContextRefreshedEvent(ApplicationContext source) {
		super(source);
	}

}
