/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.context.annotation;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ConcurrentReferenceHashMap;

/**
 * Utilities for processing {@link Bean}-annotated methods.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
abstract class BeanAnnotationHelper {

	private static final Map<Method, String> beanNameCache = new ConcurrentReferenceHashMap<>();

	private static final Map<Method, Boolean> scopedProxyCache = new ConcurrentReferenceHashMap<>();


	public static boolean isBeanAnnotated(Method method) {
		return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
	}

	public static String determineBeanNameFor(Method beanMethod, ConfigurableBeanFactory beanFactory) {
		String beanName = retrieveBeanNameFor(beanMethod);
		if (!beanName.isEmpty()) {
			return beanName;
		}
		return (beanFactory.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR)
				instanceof ConfigurationBeanNameGenerator cbng ?
				cbng.deriveBeanName(MethodMetadata.introspect(beanMethod)) : beanMethod.getName());
	}

	public static String determineBeanNameFor(Method beanMethod) {
		String beanName = retrieveBeanNameFor(beanMethod);
		return (!beanName.isEmpty() ? beanName : beanMethod.getName());
	}

	private static String retrieveBeanNameFor(Method beanMethod) {
		String beanName = beanNameCache.get(beanMethod);
		if (beanName == null) {
			// By default, the bean name is empty (indicating a name to be derived from the method name)
			beanName = "";
			// Check to see if the user has explicitly set a custom bean name...
			AnnotationAttributes bean =
					AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Bean.class, false, false);
			if (bean != null) {
				String[] names = bean.getStringArray("name");
				if (names.length > 0) {
					beanName = names[0];
				}
			}
			beanNameCache.put(beanMethod, beanName);
		}
		return beanName;
	}

	public static boolean isScopedProxy(Method beanMethod) {
		Boolean scopedProxy = scopedProxyCache.get(beanMethod);
		if (scopedProxy == null) {
			AnnotationAttributes scope =
					AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Scope.class, false, false);
			scopedProxy = (scope != null && scope.getEnum("proxyMode") != ScopedProxyMode.NO);
			scopedProxyCache.put(beanMethod, scopedProxy);
		}
		return scopedProxy;
	}

}
