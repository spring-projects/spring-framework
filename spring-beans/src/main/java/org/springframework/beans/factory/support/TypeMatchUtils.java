/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.type.TypeInformation;
import org.springframework.util.Assert;

/**
 * Utility class for {@link TypeInformation} based type matching.
 * 
 * @author Oliver Gierke
 */
public class TypeMatchUtils {

	/**
	 * Uses the native {@link AbstractBeanFactory#isTypeMatch(String, TypeInformation)} method in case the given
	 * {@link BeanFactory} is one or falls back to the plain {@link BeanFactory#isTypeMatch(String, Class)}. The latter
	 * will result in less generics-awareness for ongoing type resolution.
	 * 
	 * @param beanName must not be {@literal null} or empty.
	 * @param type must not be {@literal null}.
	 * @param beanFactory must not be {@literal null}.
	 * @return
	 */
	public static boolean isTypeMatch(String beanName, TypeInformation<?> type, BeanFactory beanFactory) {

		Assert.notNull(type, "TypeInformation must not be null!");
		Assert.notNull(beanFactory, "BeanFactory must not be null!");
		Assert.hasText(beanName, "Bean name must not be null!");

		if (beanFactory instanceof AbstractBeanFactory) {
			return ((AbstractBeanFactory) beanFactory).isTypeMatch(beanName, type);
		}

		return beanFactory.isTypeMatch(beanName, type.getType());
	}

	/**
	 * Returns the bean names for all beans matching the given {@link TypeInformation}.
	 * Will forward the {@link TypeInformation} if the given {@link BeanFactory} is a
	 * {@link DefaultListableBeanFactory} and fall back to the raw type otherwise.
	 * 
	 * @param type must not be {@literal null}.
	 * @param includeNonSingletons
	 * @param allowEagerInit
	 * @param beanFactory must not be {@literal null}.
	 * @return
	 */
	public static String[] getBeanNamesForType(TypeInformation<?> type, boolean includeNonSingletons,
			boolean allowEagerInit, ListableBeanFactory beanFactory) {
		
		Assert.notNull(type, "TypeInformation must not be null!");
		Assert.notNull(beanFactory, "ListableBeanFactory must not be null!");

		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			return ((DefaultListableBeanFactory) beanFactory).getBeanNamesForType(type, includeNonSingletons, allowEagerInit);					
		}
		
		return beanFactory.getBeanNamesForType(type.getType(), includeNonSingletons, allowEagerInit);
	}
}
