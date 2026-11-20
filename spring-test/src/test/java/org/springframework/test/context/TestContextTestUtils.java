/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import org.springframework.test.context.cache.ContextCache;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.support.DefaultBootstrapContext;

/**
 * Collection of test-related utility methods for working with {@link TestContext TestContexts}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public abstract class TestContextTestUtils {

	public static TestContext buildTestContext(Class<?> testClass, ContextCache contextCache) {
		return buildTestContext(testClass, new DefaultCacheAwareContextLoaderDelegate(contextCache));
	}

	public static TestContext buildTestContext(Class<?> testClass,
			CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {

		BootstrapContext bootstrapContext = new DefaultBootstrapContext(testClass, cacheAwareContextLoaderDelegate);
		TestContextBootstrapper testContextBootstrapper = BootstrapUtils.resolveTestContextBootstrapper(bootstrapContext);
		return testContextBootstrapper.buildTestContext();
	}

}
