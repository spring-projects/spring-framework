/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.handler.annotation.support;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.method.AbstractExceptionHandlerMethodResolver;
import org.springframework.messaging.handler.method.HandlerMethodSelector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;


/**
 * A sub-class of {@link AbstractExceptionHandlerMethodResolver} that looks for
 * {@link MessageExceptionHandler}-annotated methods in a given class. The actual
 * exception types handled are extracted either from the annotation, if present,
 * or from the method signature as a fallback option.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class AnnotationExceptionHandlerMethodResolver extends AbstractExceptionHandlerMethodResolver {


	/**
	 * A constructor that finds {@link MessageExceptionHandler} methods in the given type.
	 * @param handlerType the type to introspect
	 */
	public AnnotationExceptionHandlerMethodResolver(Class<?> handlerType) {
		super(initExceptionMappings(handlerType));
	}

	private static Map<Class<? extends Throwable>, Method> initExceptionMappings(Class<?> handlerType) {
		Map<Class<? extends Throwable>, Method> result = new HashMap<Class<? extends Throwable>, Method>();
		for (Method method : HandlerMethodSelector.selectMethods(handlerType, EXCEPTION_HANDLER_METHOD_FILTER)) {
			for (Class<? extends Throwable> exceptionType : getMappedExceptions(method)) {
				Method oldMethod = result.put(exceptionType, method);
				if (oldMethod != null && !oldMethod.equals(method)) {
					throw new IllegalStateException(
							"Ambiguous @ExceptionHandler method mapped for [" + exceptionType + "]: {" +
									oldMethod + ", " + method + "}.");
				}
			}
		}
		return result;
	}

	private static List<Class<? extends Throwable>> getMappedExceptions(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();
		MessageExceptionHandler annot = AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class);
		result.addAll(Arrays.asList(annot.value()));
		if (result.isEmpty()) {
			result.addAll(getExceptionsFromMethodSignature(method));
		}
		return result;
	}


	/** A filter for selecting annotated exception handling methods. */
	public final static MethodFilter EXCEPTION_HANDLER_METHOD_FILTER = new MethodFilter() {

		@Override
		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, MessageExceptionHandler.class) != null;
		}
	};

}
