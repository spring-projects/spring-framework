/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Factory for creating {@link ContextCustomizer ContextCustomizers}.
 *
 * <p>Factories are invoked after {@link ContextLoader ContextLoaders} have
 * processed context configuration attributes but before the
 * {@link MergedContextConfiguration} is created.
 *
 * <p>By default, the Spring TestContext Framework will use the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism for loading default factories configured in all {@code META-INF/spring.factories}
 * files on the classpath.
 *
 * <p>As of Spring Framework 6.1, it is also possible to register factories
 * declaratively via the {@link ContextCustomizerFactories @ContextCustomizerFactories}
 * annotation.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
 * @see ContextCustomizer
 * @see ContextCustomizerFactories @ContextCustomizerFactories
 */
@FunctionalInterface
public interface ContextCustomizerFactory {

	/**
	 * Create a {@link ContextCustomizer} that should be used to customize a
	 * {@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
	 * before it is refreshed.
	 * @param testClass the test class
	 * @param configAttributes the list of context configuration attributes for
	 * the test class, ordered <em>bottom-up</em> (i.e., as if we were traversing
	 * up the class hierarchy or enclosing class hierarchy); never {@code null} or empty
	 * @return a {@link ContextCustomizer} or {@code null} if no customizer should
	 * be used
	 */
	@Nullable ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
