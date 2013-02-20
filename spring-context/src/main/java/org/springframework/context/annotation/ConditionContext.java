/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.annotation;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Context information for use by {@link Condition}s.
 *
 * @author Phillip Webb
 * @since 3.2
 */
public interface ConditionContext {

	/**
	 * Returns the {@link BeanDefinitionRegistry} that will hold the bean definition
	 * should the condition match.
	 * @return the registry (never {@code null}
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * Return the {@link Environment} for which the current application is running or
	 * {@code null} if not environment is availble.
	 * @return the environment or {@code null}
	 */
	Environment getEnvironment();

	/**
	 * Returns the {@link ConfigurableListableBeanFactory} that will hold the bean
	 * definition should the conditation match. If a
	 * {@link ConfigurableListableBeanFactory} is unavailable an
	 * {@link IllegalStateException} will be thrown.
	 * @return the bean factory
	 * @throws IllegalStateException if the bean factory could not be obtained
	 */
	ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException;

	/**
	 * Returns the {@link ResourceLoader} currently being used or {@code null} if the
	 * resource loader cannot be obtained.
	 * @return a resource loader or {@code null}
	 */
	ResourceLoader getResourceLoader();

	/**
	 * Returns the {@link ClassLoader} that should be used to load additional classes
	 * or {@code null} if the default classloader should be used.
	 * @return the classloader or {@code null}
	 */
	ClassLoader getClassLoader();

}
