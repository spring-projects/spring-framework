/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context;

import java.time.Clock;
import java.util.EventObject;

/**
 * Class to be extended by all application events. Abstract as it
 * doesn't make sense for generic events to be published directly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.context.ApplicationListener
 * @see org.springframework.context.event.EventListener
 */
public abstract class ApplicationEvent extends EventObject {

	/** use serialVersionUID from Spring 1.2 for interoperability. */
	private static final long serialVersionUID = 7099057708183571937L;

	/** System time when the event happened. */
	private final long timestamp;


	/**
	 * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
	 * set to {@link System#currentTimeMillis()}.
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public ApplicationEvent(Object source) {
		super(source);
		this.timestamp = System.currentTimeMillis();
	}

	/**
	 * Create a new {@code ApplicationEvent} with its {@link #getTimestamp() timestamp}
	 * set to the value returned by {@link Clock#millis()} in the provided {@link Clock}.
	 * <p>This constructor is typically used in testing scenarios.
	 * @param source the object on which the event initially occurred or with
	 * which the event is associated (never {@code null})
	 * @param clock a clock which will provide the timestamp
	 * @since 5.3.8
	 * @see #ApplicationEvent(Object)
	 */
	public ApplicationEvent(Object source, Clock clock) {
		super(source);
		this.timestamp = clock.millis();
	}


	/**
	 * Return the time in milliseconds when the event occurred.
	 * @see #ApplicationEvent(Object)
	 * @see #ApplicationEvent(Object, Clock)
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

}
