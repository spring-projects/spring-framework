/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.beans.factory;

/**
 * Callback interface for initializing a Spring {@link ListableBeanFactory}
 * prior to entering the singleton pre-instantiation phase. Can be used to
 * trigger early initialization of specific beans before regular singletons.
 *
 * <p>Can be programmatically applied to a {@code ListableBeanFactory} instance.
 * In an {@code ApplicationContext}, beans of type {@code BeanFactoryInitializer}
 * will be autodetected and automatically applied to the underlying bean factory.
 *
 * @author Juergen Hoeller
 * @since 6.2
 * @param <F> the bean factory type
 * @see org.springframework.beans.factory.config.ConfigurableListableBeanFactory#preInstantiateSingletons()
 */
public interface BeanFactoryInitializer<F extends ListableBeanFactory> {

	/**
	 * Initialize the given bean factory.
	 * @param beanFactory the bean factory to bootstrap
	 */
	void initialize(F beanFactory);

}
