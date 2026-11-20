/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.event;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Holder class to expose the application events published during the execution
 * of a test in the form of a thread-bound {@link ApplicationEvents} object.
 *
 * <p>{@code ApplicationEvents} are registered in this holder and managed by
 * the {@link ApplicationEventsTestExecutionListener}.
 *
 * <p>Although this class is {@code public}, it is only intended for use within
 * the <em>Spring TestContext Framework</em> or in the implementation of
 * third-party extensions. Test authors should therefore allow the current
 * instance of {@code ApplicationEvents} to be
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired}
 * into a field in the test class or injected via a parameter in test and
 * lifecycle methods when using JUnit Jupiter and the {@link
 * org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @author Simon Baslé
 * @since 5.3.3
 * @see ApplicationEvents
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 */
public abstract class ApplicationEventsHolder {

	private static final ThreadLocal<DefaultApplicationEvents> applicationEvents = new InheritableThreadLocal<>();


	private ApplicationEventsHolder() {
		// no-op to prevent instantiation of this holder class
	}


	/**
	 * Get the {@link ApplicationEvents} for the current thread.
	 * @return the current {@code ApplicationEvents}, or {@code null} if not registered
	 */
	public static @Nullable ApplicationEvents getApplicationEvents() {
		return applicationEvents.get();
	}

	/**
	 * Get the {@link ApplicationEvents} for the current thread.
	 * @return the current {@code ApplicationEvents}
	 * @throws IllegalStateException if an instance of {@code ApplicationEvents}
	 * has not been registered for the current thread
	 */
	public static ApplicationEvents getRequiredApplicationEvents() {
		ApplicationEvents events = applicationEvents.get();
		Assert.state(events != null, "Failed to retrieve ApplicationEvents for the current thread. " +
				"Ensure that your test class is annotated with @RecordApplicationEvents " +
				"and that the ApplicationEventsTestExecutionListener is registered.");
		return events;
	}


	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread, if necessary.
	 * <p>If {@link #registerApplicationEvents()} has already been called for the
	 * current thread, this method does not do anything.
	 */
	static void registerApplicationEventsIfNecessary() {
		if (getApplicationEvents() == null) {
			registerApplicationEvents();
		}
	}

	/**
	 * Register a new {@link DefaultApplicationEvents} instance to be used for the
	 * current thread.
	 */
	static void registerApplicationEvents() {
		applicationEvents.set(new DefaultApplicationEvents());
	}

	/**
	 * Remove the registration of the {@link ApplicationEvents} for the current thread.
	 */
	static void unregisterApplicationEvents() {
		applicationEvents.remove();
	}

}
