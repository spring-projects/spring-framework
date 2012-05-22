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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.ExceptionDepthComparator;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.HandlerMethodSelector;

/**
 * Given a set of @{@link ExceptionHandler} methods at initialization, finds
 * the best matching method mapped to an exception at runtime.
 *
 * <p>Exception mappings are extracted from the method @{@link ExceptionHandler}
 * annotation or by looking for {@link Throwable} method arguments.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ExceptionHandlerMethodResolver {

	private static final Method NO_METHOD_FOUND = ClassUtils.getMethodIfAvailable(System.class, "currentTimeMillis");

	private final Map<Class<? extends Throwable>, Method> mappedMethods =
		new ConcurrentHashMap<Class<? extends Throwable>, Method>();

	private final Map<Class<? extends Throwable>, Method> exceptionLookupCache =
			new ConcurrentHashMap<Class<? extends Throwable>, Method>(16);

	/**
	 * A constructor that finds {@link ExceptionHandler} methods in a handler.
	 * @param handlerType the handler to inspect for exception handler methods.
	 * @throws IllegalStateException
	 * 		If an exception type is mapped to two methods.
	 * @throws IllegalArgumentException
	 * 		If an @{@link ExceptionHandler} method is not mapped to any exceptions.
	 */
	public ExceptionHandlerMethodResolver(Class<?> handlerType) {
		for (Method method : HandlerMethodSelector.selectMethods(handlerType, EXCEPTION_HANDLER_METHODS)) {
			for (Class<? extends Throwable> exceptionType : detectExceptionMappings(method)) {
				addExceptionMapping(exceptionType, method);
			}
		}
	}

	/**
	 * Extract exception mappings from the {@code @ExceptionHandler} annotation
	 * first and as a fall-back from the method signature.
	 */
	@SuppressWarnings("unchecked")
	private List<Class<? extends Throwable>> detectExceptionMappings(Method method) {
		List<Class<? extends Throwable>> result = new ArrayList<Class<? extends Throwable>>();

		ExceptionHandler annotation = AnnotationUtils.findAnnotation(method, ExceptionHandler.class);
		result.addAll(Arrays.asList(annotation.value()));

		if (result.isEmpty()) {
			for (Class<?> paramType : method.getParameterTypes()) {
				if (Throwable.class.isAssignableFrom(paramType)) {
					result.add((Class<? extends Throwable>) paramType);
				}
			}
		}

		Assert.notEmpty(result, "No exception types mapped to {" + method + "}");

		return result;
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException(
					"Ambiguous @ExceptionHandler method mapped for [" + exceptionType + "]: {" +
					oldMethod + ", " + method + "}.");
		}
	}

	/**
	 * Whether the contained type has any exception mappings.
	 */
	public boolean hasExceptionMappings() {
		return (this.mappedMethods.size() > 0);
	}

	/**
	 * Find a method to handle the given exception.
	 * Use {@link ExceptionDepthComparator} if more than one match is found.
	 * @param exception the exception
	 * @return a method to handle the exception or {@code null}
	 */
	public Method resolveMethod(Exception exception) {
		Class<? extends Exception> exceptionType = exception.getClass();
		Method method = this.exceptionLookupCache.get(exceptionType);
		if (method == null) {
			method = getMappedMethod(exceptionType);
			this.exceptionLookupCache.put(exceptionType, method != null ? method : NO_METHOD_FOUND);
		}
		return method != NO_METHOD_FOUND ? method : null;
	}

	/**
	 * Return the method mapped to the given exception type or {@code null}.
	 */
	private Method getMappedMethod(Class<? extends Exception> exceptionType) {
		List<Class<? extends Throwable>> matches = new ArrayList<Class<? extends Throwable>>();
		for(Class<? extends Throwable> mappedException : this.mappedMethods.keySet()) {
			if (mappedException.isAssignableFrom(exceptionType)) {
				matches.add(mappedException);
			}
		}
		if (!matches.isEmpty()) {
			Collections.sort(matches, new ExceptionDepthComparator(exceptionType));
			return mappedMethods.get(matches.get(0));
		}
		else {
			return null;
		}
	}

	/**
	 * A filter for selecting {@code @ExceptionHandler} methods.
	 */
	public final static MethodFilter EXCEPTION_HANDLER_METHODS = new MethodFilter() {

		public boolean matches(Method method) {
			return AnnotationUtils.findAnnotation(method, ExceptionHandler.class) != null;
		}
	};

}
