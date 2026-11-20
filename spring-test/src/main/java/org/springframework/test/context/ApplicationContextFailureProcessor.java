/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;

/**
 * Strategy for components that process failures related to application contexts
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Implementations must be registered via the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism.
 *
 * @author Sam Brannen
 * @since 6.0
 * @see ContextLoadException
 */
public interface ApplicationContextFailureProcessor {

	/**
	 * Invoked when a failure was encountered while attempting to load an
	 * {@link ApplicationContext}.
	 * <p>Implementations of this method must not throw any exceptions. Consequently,
	 * any exception thrown by an implementation of this method will be ignored, though
	 * potentially logged.
	 * @param context the application context that did not load successfully
	 * @param exception the exception thrown while loading the application context
	 */
	void processLoadFailure(ApplicationContext context, @Nullable Throwable exception);

}
