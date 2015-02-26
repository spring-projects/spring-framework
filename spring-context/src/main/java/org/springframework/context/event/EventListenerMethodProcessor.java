/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Register {@link EventListener} annotated method as individual {@link ApplicationListener}
 * instances.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
public class EventListenerMethodProcessor implements SmartInitializingSingleton, ApplicationContextAware {

	protected final Log logger = LogFactory.getLog(getClass());

	private ConfigurableApplicationContext applicationContext;

	private final EventExpressionEvaluator evaluator = new EventExpressionEvaluator();

	private final Set<Class<?>> nonAnnotatedClasses =
			Collections.newSetFromMap(new ConcurrentHashMap<Class<?>, Boolean>(64));

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext,
				"ApplicationContext does not implement ConfigurableApplicationContext");
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;

	}

	/**
	 * Return the {@link EventListenerFactory} instances to use to handle {@link EventListener}
	 * annotated methods.
	 */
	protected List<EventListenerFactory> getEventListenerFactories() {
		Map<String, EventListenerFactory> beans =
				this.applicationContext.getBeansOfType(EventListenerFactory.class);
		List<EventListenerFactory> allFactories = new ArrayList<EventListenerFactory>(beans.values());
		AnnotationAwareOrderComparator.sort(allFactories);
		return allFactories;
	}

	@Override
	public void afterSingletonsInstantiated() {
		List<EventListenerFactory> factories = getEventListenerFactories();
		String[] allBeanNames = this.applicationContext.getBeanNamesForType(Object.class);
		for (String beanName : allBeanNames) {
			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
				Class<?> type = this.applicationContext.getType(beanName);
				try {
					processBean(factories, beanName, type);
				}
				catch (RuntimeException e) {
					throw new BeanInitializationException("Failed to process @EventListener " +
							"annotation on bean with name '" + beanName + "'", e);
				}
			}
		}
	}

	protected void processBean(List<EventListenerFactory> factories, String beanName, final Class<?> type) {
		Class<?> targetType = getTargetClass(beanName, type);
		if (!this.nonAnnotatedClasses.contains(targetType)) {
			final Set<Method> annotatedMethods = new LinkedHashSet<Method>(1);
			Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(targetType);
			for (Method method : methods) {
				EventListener eventListener = AnnotationUtils.findAnnotation(method, EventListener.class);
				if (eventListener == null) {
					continue;
				}
				for (EventListenerFactory factory : factories) {
					if (factory.supportsMethod(method)) {
						if (!type.equals(targetType)) {
							method = getProxyMethod(type, method);
						}
						ApplicationListener<?> applicationListener =
								factory.createApplicationListener(beanName, type, method);
						if (applicationListener instanceof ApplicationListenerMethodAdapter) {
							((ApplicationListenerMethodAdapter)applicationListener)
									.init(this.applicationContext, this.evaluator);
						}
						this.applicationContext.addApplicationListener(applicationListener);
						annotatedMethods.add(method);
						break;
					}
				}
			}
			if (annotatedMethods.isEmpty()) {
				this.nonAnnotatedClasses.add(type);
				if (logger.isTraceEnabled()) {
					logger.trace("No @EventListener annotations found on bean class: " + type);
				}
			}
			else {
				// Non-empty set of methods
				if (logger.isDebugEnabled()) {
					logger.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" + beanName +
							"': " + annotatedMethods);
				}
			}
		}
	}

	private Class<?> getTargetClass(String beanName, Class<?> type) {
		if (SpringProxy.class.isAssignableFrom(type)) {
			Object bean = this.applicationContext.getBean(beanName);
			return AopUtils.getTargetClass(bean);
		}
		else {
			return type;
		}
	}

	private Method getProxyMethod(Class<?> proxyType, Method method) {
		try {
			// Found a @EventListener method on the target class for this JDK proxy ->
			// is it also present on the proxy itself?
			return proxyType.getMethod(method.getName(), method.getParameterTypes());
		}
		catch (SecurityException ex) {
			ReflectionUtils.handleReflectionException(ex);
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalStateException(String.format(
					"@EventListener method '%s' found on bean target class '%s', " +
							"but not found in any interface(s) for bean JDK proxy. Either " +
							"pull the method up to an interface or switch to subclass (CGLIB) " +
							"proxies by setting proxy-target-class/proxyTargetClass " +
							"attribute to 'true'", method.getName(), method.getDeclaringClass().getSimpleName()));
		}
		return null;
	}

}
