/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import org.springframework.context.ApplicationContext;

/**
 * Exception thrown when an error occurs while a {@link SmartContextLoader}
 * attempts to load an {@link ApplicationContext}.
 *
 * <p>This exception provides access to the {@linkplain #getApplicationContext()
 * application context} that failed to load as well as the {@linkplain #getCause()
 * exception} caught while attempting to load that context.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see SmartContextLoader#loadContext(MergedContextConfiguration)
 */
@SuppressWarnings("serial")
public class ContextLoadException extends Exception {

	private final ApplicationContext applicationContext;


	/**
	 * Create a new {@code ContextLoadException} for the supplied
	 * {@link ApplicationContext} and {@link Throwable}.
	 * @param applicationContext the application context that failed to load
	 * @param cause the exception caught while attempting to load that context
	 */
	public ContextLoadException(ApplicationContext applicationContext, Throwable cause) {
		super(cause);
		this.applicationContext = applicationContext;
	}


	/**
	 * Get the {@code ApplicationContext} that failed to load.
	 * <p>Clients must not retain a long-lived reference to the context returned
	 * from this method.
	 */
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

}
