/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import java.util.List;

import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.config.interfaces.BootstrapWithTestInterface.CustomTestContextBootstrapper;
import org.springframework.test.context.support.DefaultTestContextBootstrapper;

import static java.util.Collections.singletonList;

/**
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.3
 */
@BootstrapWith(CustomTestContextBootstrapper.class)
interface BootstrapWithTestInterface {

	class CustomTestContextBootstrapper extends DefaultTestContextBootstrapper {

		@Override
		protected List<ContextCustomizerFactory> getContextCustomizerFactories() {
			return singletonList((testClass, configAttributes) ->
					(context,mergedConfig) -> context.getBeanFactory().registerSingleton("foo", "foo"));
		}
	}

}
