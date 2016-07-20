/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import java.util.List;

/**
 * Factory for creating {@link ContextCustomizer ContextCustomizers}.
 *
 * <p>Factories are invoked after {@link ContextLoader ContextLoaders} have
 * processed context configuration attributes but before the
 * {@link MergedContextConfiguration} is created.
 *
 * <p>By default, the Spring TestContext Framework will use the
 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
 * mechanism for loading factories configured in all {@code META-INF/spring.factories}
 * files on the classpath.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 4.3
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
	 * up the class hierarchy); never {@code null} or empty
	 * @return a {@link ContextCustomizer} or {@code null} if no customizer should
	 * be used
	 */
	ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes);

}
