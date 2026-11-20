/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;

/**
 * @author Sam Brannen
 * @since 6.2.4
 */
class AbstractAotContextLoader extends AbstractContextLoader implements AotContextLoader {

	@Override
	public final GenericApplicationContext loadContext(MergedContextConfiguration mergedConfig) {
		return new StaticApplicationContext();
	}

	@Override
	public final GenericApplicationContext loadContextForAotRuntime(MergedContextConfiguration mergedConfig,
			ApplicationContextInitializer<ConfigurableApplicationContext> initializer) {

		return loadContext(mergedConfig);
	}

	@Override
	protected final String getResourceSuffix() {
		throw new UnsupportedOperationException();
	}

}
