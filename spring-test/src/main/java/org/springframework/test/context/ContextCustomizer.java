/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Strategy interface for customizing {@link ConfigurableApplicationContext
 * application contexts} that are created and managed by the <em>Spring
 * TestContext Framework</em>.
 *
 * <p>Customizers are created by {@link ContextCustomizerFactory} implementations.
 *
 * <p><strong>WARNING</strong>: implementations must implement correct
 * {@link Object#equals(Object) equals} and {@link Object#hashCode() hashCode}
 * methods since customizers form part of the {@link MergedContextConfiguration}
 * which is used as a cache key.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 * @see ContextCustomizerFactory
 * @see ContextCustomizerFactories @ContextCustomizerFactories
 * @see org.springframework.test.context.support.AbstractContextLoader#customizeContext
 */
@FunctionalInterface
public interface ContextCustomizer {

	/**
	 * Customize the supplied {@code ConfigurableApplicationContext} <em>after</em>
	 * bean definitions have been loaded into the context but <em>before</em> the
	 * context has been refreshed.
	 * @param context the context to customize
	 * @param mergedConfig the merged context configuration
	 */
	void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig);

}
