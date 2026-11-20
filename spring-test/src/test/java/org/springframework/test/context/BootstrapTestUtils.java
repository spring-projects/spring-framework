/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import org.springframework.test.context.support.DefaultBootstrapContext;

/**
 * Collection of test-related utility methods for working with {@link BootstrapContext
 * BootstrapContexts} and {@link TestContextBootstrapper TestContextBootstrappers}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public abstract class BootstrapTestUtils {

	private BootstrapTestUtils() {
		/* no-op */
	}

	public static BootstrapContext buildBootstrapContext(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		return new DefaultBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
	}

	public static TestContextBootstrapper resolveTestContextBootstrapper(BootstrapContext bootstrapContext) {
		return BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
	}

}
