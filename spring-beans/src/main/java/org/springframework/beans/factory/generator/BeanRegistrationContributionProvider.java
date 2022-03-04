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

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.lang.Nullable;

/**
 * Strategy interface to be implemented by components that require custom
 * contribution for a bean definition.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
@FunctionalInterface
public interface BeanRegistrationContributionProvider {

	/**
	 * Return the {@link BeanFactoryContribution} that is capable of contributing
	 * the registration of a bean for the given {@link RootBeanDefinition} or
	 * {@code null} if the specified bean definition is not supported.
	 * @param beanName the bean name to handle
	 * @param beanDefinition the merged bean definition
	 * @return a contribution for the specified bean definition or {@code null}
	 */
	@Nullable
	BeanFactoryContribution getContributionFor(String beanName, RootBeanDefinition beanDefinition);

}
