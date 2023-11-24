/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.aot;

import org.springframework.aot.generate.GeneratedMethods;
import org.springframework.aot.generate.MethodReference;

/**
 * Interface that can be used to configure the code that will be generated to
 * perform bean factory initialization.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see BeanFactoryInitializationAotContribution
 */
public interface BeanFactoryInitializationCode {

	/**
	 * The recommended variable name to use to refer to the bean factory.
	 */
	String BEAN_FACTORY_VARIABLE = "beanFactory";

	/**
	 * Get the {@link GeneratedMethods} used by the initializing code.
	 * @return the generated methods
	 */
	GeneratedMethods getMethods();

	/**
	 * Add an initializer method call. An initializer can use a flexible signature,
	 * using any of the following:
	 * <ul>
	 * <li>{@code DefaultListableBeanFactory}, or {@code ConfigurableListableBeanFactory}
	 * to use the bean factory.</li>
	 * <li>{@code ConfigurableEnvironment} or {@code Environment} to access the
	 * environment.</li>
	 * <li>{@code ResourceLoader} to load resources.</li>
	 * </ul>
	 * @param methodReference a reference to the initialize method to call.
	 */
	void addInitializer(MethodReference methodReference);

}
