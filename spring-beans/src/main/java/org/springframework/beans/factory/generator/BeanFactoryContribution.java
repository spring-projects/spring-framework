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

import java.util.function.BiPredicate;

import org.springframework.beans.factory.config.BeanDefinition;

/**
 * Contribute optimizations ahead of time to initialize a bean factory.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public interface BeanFactoryContribution {

	/**
	 * Contribute ahead of time optimizations to the specific
	 * {@link BeanFactoryInitialization}.
	 * @param initialization {@link BeanFactoryInitialization} to contribute to
	 */
	void applyTo(BeanFactoryInitialization initialization);

	/**
	 * Return a predicate that determines if a particular bean definition
	 * should be excluded from processing. Can be used to exclude infrastructure
	 * that has been optimized using generated code.
	 * @return the predicate to use
	 */
	default BiPredicate<String, BeanDefinition> getBeanDefinitionExcludeFilter() {
		return (beanName, beanDefinition) -> false;
	}

}
