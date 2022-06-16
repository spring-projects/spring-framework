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

package org.springframework.beans.factory.generator.config;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Resolve the attributes of an injected element such as a {@code Constructor}
 * or a factory {@code Method}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface InjectedElementResolver {

	/**
	 * Resolve the attributes using the specified bean factory.
	 * @param beanFactory the bean factory to use
	 * @return the resolved attributes
	 */
	default InjectedElementAttributes resolve(DefaultListableBeanFactory beanFactory) {
		return resolve(beanFactory, true);
	}

	/**
	 * Resolve the attributes using the specified bean factory.
	 * @param beanFactory the bean factory to use
	 * @param required whether the injection point is mandatory
	 * @return the resolved attributes
	 */
	InjectedElementAttributes resolve(DefaultListableBeanFactory beanFactory, boolean required);

	/**
	 * Invoke the specified consumer with the resolved
	 * {@link InjectedElementAttributes attributes}.
	 * @param beanFactory the bean factory to use to resolve the attributes
	 * @param attributes a consumer of the resolved attributes
	 */
	default void invoke(DefaultListableBeanFactory beanFactory,
			BeanDefinitionRegistrar.ThrowableConsumer<InjectedElementAttributes> attributes) {

		InjectedElementAttributes elements = resolve(beanFactory);
		attributes.accept(elements);
	}

	/**
	 * Create an instance based on the resolved
	 * {@link InjectedElementAttributes attributes}.
	 * @param beanFactory the bean factory to use to resolve the attributes
	 * @param factory a factory to create the instance based on the resolved attributes
	 * @param <T> the type of the instance
	 * @return a new instance
	 */
	default <T> T create(DefaultListableBeanFactory beanFactory,
			BeanDefinitionRegistrar.ThrowableFunction<InjectedElementAttributes, T> factory) {

		InjectedElementAttributes attributes = resolve(beanFactory);
		return factory.apply(attributes);
	}

}
