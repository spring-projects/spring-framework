/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;

/**
 * Strategy interface for determining whether a specific bean definition
 * qualifies as an autowire candidate for a specific dependency.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public interface AutowireCandidateResolver {

	/**
	 * Determine whether the given bean definition qualifies as an
	 * autowire candidate for the given dependency.
	 * @param bdHolder the bean definition including bean name and aliases
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return whether the bean definition qualifies as autowire candidate
	 */
	boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor);

	/**
	 * Determine whether a default value is suggested for the given dependency.
	 * @param descriptor the descriptor for the target method parameter or field
	 * @return the value suggested (typically an expression String),
	 * or {@code null} if none found
	 * @since 3.0
	 */
	Object getSuggestedValue(DependencyDescriptor descriptor);

}
