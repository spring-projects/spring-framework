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
 * Context passed to {@link ContextCustomizerFactory ContextCustomizerFactories}.
 *
 * @author Phillip Webb
 * @since 4.3
 */
public interface ContextCustomizerFactoryContext {

	/**
	 * Get the {@linkplain Class test class}.
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * Get the list of context configuration attributes for the test class, ordered
	 * <em>bottom-up</em> (i.e., as if we were traversing up the class hierarchy); never
	 * {@code null} or empty.
	 *
	 * @return the configuration attributes
	 */
	List<ContextConfigurationAttributes> getConfigurationAttributes();

	/**
	 * Return if any of the {@link ContextConfigurationAttributes} have
	 * {@link ContextConfigurationAttributes#hasResources() resources}.
	 * @return {@code true} if at least on {@link ContextConfigurationAttributes} has
	 * resources.
	 */
	boolean hasResources();

	/**
	 * Return if any of the {@link ContextConfigurationAttributes} have
	 * {@link ContextConfigurationAttributes#getInitializers() initializer}.
	 * @return {@code true} if at least on {@link ContextConfigurationAttributes} has a
	 * initializer.
	 */
	boolean hasInitializers();

	/**
	 * Get the complete list of resource locations that were collected from all
	 * {@link #getConfigurationAttributes() ContextConfigurationAttributess}.
	 * @return the resource locations
	 * @see ContextConfigurationAttributes#getLocations()
	 */
	List<String> getLocations();

	/**
	 * Get the complete list of the annotated classes that were collected from all
	 * {@link #getConfigurationAttributes() ContextConfigurationAttributess}.
	 * @return the annotated classes
	 * @see ContextConfigurationAttributes#getClasses()
	 */
	List<Class<?>> getClasses();

	/**
	 * Get the complete list of {@code ApplicationContextInitializer} classes that were
	 * collected from all {@link #getConfigurationAttributes()
	 * ContextConfigurationAttributess}.
	 * @return the {@code ApplicationContextInitializer} classes
	 * @see ContextConfigurationAttributes#getInitializers()
	 */
	List<Class<?>> getInitializers();

}
