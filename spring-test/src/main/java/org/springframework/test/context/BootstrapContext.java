/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

/**
 * {@code BootstrapContext} encapsulates the context in which the <em>Spring
 * TestContext Framework</em> is bootstrapped.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see TestContextBootstrapper
 */
public interface BootstrapContext {

	/**
	 * Get the {@linkplain Class test class} for this bootstrap context.
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
	 * interaction with the {@code ContextCache}.
	 * @return the context loader delegate (never {@code null})
	 */
	CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
