/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.beans.factory.support;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * {@link AutowireCandidateResolver} implementation to use when no annotation
 * support is available. This implementation checks the bean definition only.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 2.5
 */
public class SimpleAutowireCandidateResolver implements AutowireCandidateResolver {

	/**
	 * Shared instance of {@code SimpleAutowireCandidateResolver}.
	 * @since 5.2.7
	 */
	public static final SimpleAutowireCandidateResolver INSTANCE = new SimpleAutowireCandidateResolver();

	/**
	 * This implementation returns {@code this} as-is.
	 * @see #INSTANCE
	 */
	@Override
	public AutowireCandidateResolver cloneIfNecessary() {
		return this;
	}


	/**
	 * Resolve a map of all beans of the given type, also picking up beans defined in
	 * ancestor bean factories, with the specific condition that each bean actually
	 * has autowire candidate status. This matches simple injection point resolution
	 * as implemented by this {@link AutowireCandidateResolver} strategy, including
	 * beans which are not marked as default candidates but excluding beans which
	 * are not even marked as autowire candidates.
	 * @param lbf the bean factory
	 * @param type the type of bean to match
	 * @return the Map of matching bean instances, or an empty Map if none
	 * @throws BeansException if a bean could not be created
	 * @since 6.2.3
	 * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(ListableBeanFactory, Class)
	 * @see org.springframework.beans.factory.config.BeanDefinition#isAutowireCandidate()
	 * @see AbstractBeanDefinition#isDefaultCandidate()
	 */
	public static <T> Map<String, T> resolveAutowireCandidates(ConfigurableListableBeanFactory lbf, Class<T> type) {
		Map<String, T> candidates = new LinkedHashMap<>();
		for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(lbf, type)) {
			if (AutowireUtils.isAutowireCandidate(lbf, beanName)) {
				candidates.put(beanName, lbf.getBean(beanName, type));
			}
		}
		return candidates;
	}

}
