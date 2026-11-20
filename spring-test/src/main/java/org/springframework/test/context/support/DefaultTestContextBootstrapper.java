/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.springframework.test.context.ContextLoader;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * Default implementation of the {@link TestContextBootstrapper} SPI.
 *
 * <p>Uses {@link DelegatingSmartContextLoader} as the default {@link ContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultTestContextBootstrapper extends AbstractTestContextBootstrapper {

	/**
	 * Returns {@link DelegatingSmartContextLoader}.
	 */
	@Override
	protected Class<? extends ContextLoader> getDefaultContextLoaderClass(Class<?> testClass) {
		return DelegatingSmartContextLoader.class;
	}

}
