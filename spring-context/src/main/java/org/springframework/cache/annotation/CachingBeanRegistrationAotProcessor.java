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

package org.springframework.cache.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.aop.config.AopConfigUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link BeanRegistrationAotProcessor} to register runtime hints for beans that use caching annotations to
 * enable JDK proxy creation when needed.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
public class CachingBeanRegistrationAotProcessor implements BeanRegistrationAotProcessor {

	private static final Set<Class<? extends Annotation>> CACHE_OPERATION_ANNOTATIONS = new LinkedHashSet<>(8);

	static {
		CACHE_OPERATION_ANNOTATIONS.add(Cacheable.class);
		CACHE_OPERATION_ANNOTATIONS.add(CacheEvict.class);
		CACHE_OPERATION_ANNOTATIONS.add(CachePut.class);
		CACHE_OPERATION_ANNOTATIONS.add(Caching.class);
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		if (isCaching(registeredBean.getBeanClass()) && !isClassProxyingForced(registeredBean.getBeanFactory())) {
			return (generationContext, beanRegistrationCode) -> registerSpringProxy(registeredBean.getBeanClass(),
					generationContext.getRuntimeHints());
		}
		return null;
	}

	private static boolean isClassProxyingForced(ConfigurableListableBeanFactory beanFactory) {
		return beanFactory.containsBean(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME) &&
				Boolean.TRUE.equals(beanFactory.getBeanDefinition(AopConfigUtils.AUTO_PROXY_CREATOR_BEAN_NAME)
						.getPropertyValues().get("proxyTargetClass"));
	}

	private boolean isCaching(Class<?> beanClass) {
		if (!AnnotationUtils.isCandidateClass(beanClass, CACHE_OPERATION_ANNOTATIONS)) {
			return false;
		}
		Set<AnnotatedElement> elements = new LinkedHashSet<>();
		elements.add(beanClass);
		ReflectionUtils.doWithMethods(beanClass, elements::add);
		for (Class<?> interfaceClass : ClassUtils.getAllInterfacesForClass(beanClass)) {
			elements.add(interfaceClass);
			ReflectionUtils.doWithMethods(interfaceClass, elements::add);
		}
		return elements.stream().anyMatch(element -> {
			MergedAnnotations mergedAnnotations = MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
			return CACHE_OPERATION_ANNOTATIONS.stream().anyMatch(mergedAnnotations::isPresent);
		});
	}

	private static void registerSpringProxy(Class<?> type, RuntimeHints runtimeHints) {
		Class<?>[] proxyInterfaces = ClassUtils.getAllInterfacesForClass(type);
		if (proxyInterfaces.length == 0) {
			return;
		}
		runtimeHints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(proxyInterfaces));
	}
}
