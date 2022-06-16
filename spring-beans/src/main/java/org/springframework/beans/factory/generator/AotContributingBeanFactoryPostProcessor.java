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

package org.springframework.beans.factory.generator;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.lang.Nullable;

/**
 * Specialization of {@link BeanFactoryPostProcessor} that contributes bean
 * factory optimizations ahead of time, using generated code that replaces
 * runtime behavior.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface AotContributingBeanFactoryPostProcessor extends BeanFactoryPostProcessor {

	/**
	 * Contribute a {@link BeanFactoryContribution} for the given bean factory,
	 * if applicable.
	 * @param beanFactory the bean factory to optimize
	 * @return the contribution to use or {@code null}
	 */
	@Nullable
	BeanFactoryContribution contribute(ConfigurableListableBeanFactory beanFactory);

	@Override
	default void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}

}
