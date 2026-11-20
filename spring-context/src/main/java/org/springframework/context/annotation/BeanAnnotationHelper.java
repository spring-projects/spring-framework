/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		if (beanFactory.getSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR)
				instanceof ConfigurationBeanNameGenerator cbng) {
			return cbng.deriveBeanName(MethodMetadata.introspect(beanMethod), (!beanName.isEmpty() ? beanName : null));
		}
		return determineBeanNameFrom(beanName, beanMethod);
	}

	public static String determineBeanNameFor(Method beanMethod) {
		return determineBeanNameFrom(retrieveBeanNameFor(beanMethod), beanMethod);
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

	private static String determineBeanNameFrom(String derivedBeanName, Method beanMethod) {
		return (!derivedBeanName.isEmpty() ? derivedBeanName : beanMethod.getName());
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

	static void clearCaches() {
		scopedProxyCache.clear();
		beanNameCache.clear();
	}

}
