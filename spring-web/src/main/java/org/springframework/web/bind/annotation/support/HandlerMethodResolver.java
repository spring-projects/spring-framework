/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.bind.annotation.support;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

/**
 * Support class for resolving web method annotations in a handler type.
 * Processes {@code @RequestMapping}, {@code @InitBinder},
 * {@code @ModelAttribute} and {@code @SessionAttributes}.
 *
 * <p>Used by {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}
 * and {@link org.springframework.web.portlet.mvc.annotation.AnnotationMethodHandlerAdapter}.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.web.bind.annotation.RequestMapping
 * @see org.springframework.web.bind.annotation.InitBinder
 * @see org.springframework.web.bind.annotation.ModelAttribute
 * @see org.springframework.web.bind.annotation.SessionAttributes
 */
public class HandlerMethodResolver {

	private final Set<Method> handlerMethods = new LinkedHashSet<Method>();

	private final Set<Method> initBinderMethods = new LinkedHashSet<Method>();

	private final Set<Method> modelAttributeMethods = new LinkedHashSet<Method>();

	private RequestMapping typeLevelMapping;

	private boolean sessionAttributesFound;

	private final Set<String> sessionAttributeNames = new HashSet<String>();

	private final Set<Class> sessionAttributeTypes = new HashSet<Class>();

	private final Set<String> actualSessionAttributeNames =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(4));


	/**
	 * Initialize a new HandlerMethodResolver for the specified handler type.
	 * @param handlerType the handler class to introspect
	 */
	public void init(final Class<?> handlerType) {
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
				public void doWith(Method method) {
					Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
					Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
					if (isHandlerMethod(specificMethod) &&
							(bridgedMethod == specificMethod || !isHandlerMethod(bridgedMethod))) {
						handlerMethods.add(specificMethod);
					}
					else if (isInitBinderMethod(specificMethod) &&
							(bridgedMethod == specificMethod || !isInitBinderMethod(bridgedMethod))) {
						initBinderMethods.add(specificMethod);
					}
					else if (isModelAttributeMethod(specificMethod) &&
							(bridgedMethod == specificMethod || !isModelAttributeMethod(bridgedMethod))) {
						modelAttributeMethods.add(specificMethod);
					}
				}
			}, ReflectionUtils.USER_DECLARED_METHODS);
		}
		this.typeLevelMapping = AnnotationUtils.findAnnotation(handlerType, RequestMapping.class);
		SessionAttributes sessionAttributes = AnnotationUtils.findAnnotation(handlerType, SessionAttributes.class);
		this.sessionAttributesFound = (sessionAttributes != null);
		if (this.sessionAttributesFound) {
			this.sessionAttributeNames.addAll(Arrays.asList(sessionAttributes.value()));
			this.sessionAttributeTypes.addAll(Arrays.asList(sessionAttributes.types()));
		}
	}

	protected boolean isHandlerMethod(Method method) {
		return AnnotationUtils.findAnnotation(method, RequestMapping.class) != null;
	}

	protected boolean isInitBinderMethod(Method method) {
		return AnnotationUtils.findAnnotation(method, InitBinder.class) != null;
	}

	protected boolean isModelAttributeMethod(Method method) {
		return AnnotationUtils.findAnnotation(method, ModelAttribute.class) != null;
	}


	public final boolean hasHandlerMethods() {
		return !this.handlerMethods.isEmpty();
	}

	public final Set<Method> getHandlerMethods() {
		return this.handlerMethods;
	}

	public final Set<Method> getInitBinderMethods() {
		return this.initBinderMethods;
	}

	public final Set<Method> getModelAttributeMethods() {
		return this.modelAttributeMethods;
	}

	public boolean hasTypeLevelMapping() {
		return (this.typeLevelMapping != null);
	}

	public RequestMapping getTypeLevelMapping() {
		return this.typeLevelMapping;
	}

	public boolean hasSessionAttributes() {
		return this.sessionAttributesFound;
	}

	public boolean isSessionAttribute(String attrName, Class attrType) {
		if (this.sessionAttributeNames.contains(attrName) || this.sessionAttributeTypes.contains(attrType)) {
			this.actualSessionAttributeNames.add(attrName);
			return true;
		}
		else {
			return false;
		}
	}

	public Set<String> getActualSessionAttributeNames() {
		return this.actualSessionAttributeNames;
	}

}
