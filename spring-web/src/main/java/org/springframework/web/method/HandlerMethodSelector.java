/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.method;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * Defines the algorithm for searching handler methods exhaustively including interfaces and parent
 * classes while also dealing with parameterized methods as well as interface and class-based proxies.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public abstract class HandlerMethodSelector {

	/**
	 * Selects handler methods for the given handler type. Callers of this method define handler methods
	 * of interest through the {@link MethodFilter} parameter.
	 *
	 * @param handlerType the handler type to search handler methods on
	 * @param handlerMethodFilter a {@link MethodFilter} to help recognize handler methods of interest
	 * @return the selected methods, or an empty set
	 */
	public static Set<Method> selectMethods(final Class<?> handlerType, final MethodFilter handlerMethodFilter) {
		final Set<Method> handlerMethods = new LinkedHashSet<Method>();
		Set<Class<?>> handlerTypes = new LinkedHashSet<Class<?>>();
		Class<?> specificHandlerType = null;
		if (!Proxy.isProxyClass(handlerType)) {
			handlerTypes.add(handlerType);
			specificHandlerType = handlerType;
		}
		handlerTypes.addAll(Arrays.asList(handlerType.getInterfaces()));
		for (Class<?> currentHandlerType : handlerTypes) {
			final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);
			ReflectionUtils.doWithMethods(currentHandlerType, new ReflectionUtils.MethodCallback() {
				@Override
				public void doWith(Method method) {
					Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (handlerMethodFilter.matches(specificMethod) &&
							(bridgedMethod == specificMethod || !handlerMethodFilter.matches(bridgedMethod))) {
						handlerMethods.add(specificMethod);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		return handlerMethods;
	}

}
