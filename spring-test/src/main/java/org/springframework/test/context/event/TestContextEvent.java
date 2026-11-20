/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.TestContext;

/**
 * Base class for events published by the {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
@SuppressWarnings("serial")
public abstract class TestContextEvent extends ApplicationEvent {

	/**
	 * Create a new {@code TestContextEvent}.
	 * @param source the {@code TestContext} associated with this event
	 * (must not be {@code null})
	 */
	public TestContextEvent(TestContext source) {
		super(source);
	}

	/**
	 * Get the {@link TestContext} associated with this event.
	 * @return the {@code TestContext} associated with this event (never {@code null})
	 * @see #getTestContext()
	 */
	@Override
	public final TestContext getSource() {
		return (TestContext) super.getSource();
	}

	/**
	 * Alias for {@link #getSource()}.
	 * <p>This method may be favored over {@code getSource()} &mdash; for example,
	 * to improve readability in SpEL expressions for event processing
	 * {@linkplain org.springframework.context.event.EventListener#condition conditions}.
	 * @return the {@code TestContext} associated with this event (never {@code null})
	 * @see #getSource()
	 */
	public final TestContext getTestContext() {
		return getSource();
	}

}
