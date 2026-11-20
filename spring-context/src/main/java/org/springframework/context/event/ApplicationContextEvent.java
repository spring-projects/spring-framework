/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * Base class for events raised for an {@link ApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("serial")
public abstract class ApplicationContextEvent extends ApplicationEvent {

	/**
	 * Create a new {@code ApplicationContextEvent}.
	 * @param source the {@link ApplicationContext} that the event is raised for
	 * (must not be {@code null})
	 */
	public ApplicationContextEvent(ApplicationContext source) {
		super(source);
	}

	/**
	 * Get the {@link ApplicationContext} that the event was raised for.
	 * @return the {@code ApplicationContext} that the event was raised for
	 * @since 7.0
	 * @see #getApplicationContext()
	 */
	@Override
	public ApplicationContext getSource() {
		return getApplicationContext();
	}

	/**
	 * Get the {@link ApplicationContext} that the event was raised for.
	 * @see #getSource()
	 */
	public final ApplicationContext getApplicationContext() {
		return (ApplicationContext) super.getSource();
	}

}
